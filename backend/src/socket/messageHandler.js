const { Message, Chat, User } = require('../models');
const { sendPushToUsers } = require('../services/pushService');
const ogs = require('open-graph-scraper');

function messageHandler(io, socket) {
  const userId = socket.userId;

  // Join all chat rooms for this user
  Chat.find({ members: userId }).then(chats => {
    chats.forEach(chat => socket.join(`chat:${chat._id}`));
  });

  // ── Send message ────────────────────────────────────────────────────────────
  socket.on('message:send', async (data) => {
    try {
      const { id, chatId, type, content, mediaUrl, replyToId, isForwarded, scheduledAt } = data;

      // Validate membership
      const chat = await Chat.findById(chatId).populate('members', '_id settings fcmTokens ghostMode');
      if (!chat || !chat.members.find(m => m._id.toString() === userId)) return;

      // Extract link preview for text messages
      let linkPreview;
      if (type === 'text' && content) {
        const urlMatch = content.match(/https?:\/\/[^\s]+/);
        if (urlMatch) {
          try {
            const { result } = await ogs({ url: urlMatch[0] });
            linkPreview = {
              title: result.ogTitle,
              description: result.ogDescription,
              imageUrl: result.ogImage?.[0]?.url,
              url: urlMatch[0]
            };
          } catch { /* skip preview on failure */ }
        }
      }

      const message = await Message.create({
        _id: id, // use client-provided ID for idempotency
        chat: chatId,
        sender: userId,
        type: type || 'text',
        content,
        mediaUrl,
        replyTo: replyToId || undefined,
        isForwarded: !!isForwarded,
        linkPreview,
        scheduledAt: scheduledAt ? new Date(scheduledAt) : undefined
      });

      const populated = await message.populate('sender', 'name avatarUrl');

      // Broadcast to chat room (all members including sender for sync)
      io.to(`chat:${chatId}`).emit('message:receive', {
        ...populated.toObject(),
        chatId
      });

      // Update chat's lastMessage
      await Chat.findByIdAndUpdate(chatId, { lastMessage: message._id });

      // Ack to sender: message is sent
      socket.emit('message:status', { messageId: id, status: 'sent' });

      // Push notifications to offline members
      const offlineMembers = chat.members.filter(m => {
        if (m._id.toString() === userId) return false;
        const socketRooms = io.sockets.adapter.rooms.get(`user:${m._id}`);
        return !socketRooms || socketRooms.size === 0;
      });

      if (offlineMembers.length > 0) {
        const fcmTokens = offlineMembers.flatMap(m => m.fcmTokens || []);
        if (fcmTokens.length > 0) {
          const sender = await User.findById(userId).select('name');
          await sendPushToUsers(fcmTokens, {
            chatId,
            senderName: sender.name,
            content: type === 'text' ? content : `Sent a ${type}`,
            type
          });
        }
      }

      // Mark as delivered to all online members
      const onlineMembers = chat.members.filter(m => {
        if (m._id.toString() === userId) return false;
        return io.sockets.adapter.rooms.has(`user:${m._id}`);
      });
      for (const member of onlineMembers) {
        io.to(`user:${member._id}`).emit('message:status', { messageId: id, status: 'delivered' });
      }

    } catch (err) {
      console.error('message:send error', err);
      socket.emit('message:error', { error: err.message });
    }
  });

  // ── Mark chat as read ────────────────────────────────────────────────────────
  socket.on('chat:read', async ({ chatId }) => {
    try {
      // Mark all unread messages in this chat as read
      const result = await Message.updateMany(
        { chat: chatId, readBy: { $ne: userId }, sender: { $ne: userId } },
        { $addToSet: { readBy: userId } }
      );

      if (result.modifiedCount > 0) {
        // Notify senders that their messages were read
        const messages = await Message.find({
          chat: chatId,
          'readBy': userId,
          sender: { $ne: userId }
        }).distinct('sender');

        messages.forEach(senderId => {
          io.to(`user:${senderId}`).emit('chat:read_receipt', { chatId, readBy: userId });
        });
      }
    } catch (err) { console.error('chat:read error', err); }
  });

  // ── Delete message ───────────────────────────────────────────────────────────
  socket.on('message:delete', async ({ messageId, deleteFor }) => {
    try {
      const message = await Message.findById(messageId).populate('chat');
      if (!message) return;

      if (deleteFor === 'everyone') {
        if (message.sender.toString() !== userId) return;
        const windowMs = parseInt(process.env.REVOKE_WINDOW_DAYS || '7') * 86400_000;
        if (Date.now() - message.createdAt > windowMs) {
          return socket.emit('message:error', { error: 'Revoke window expired' });
        }
        message.deletedForEveryone = true;
        message.content = '';
        message.mediaUrl = undefined;
        message.revokedAt = new Date();
        await message.save();

        // Emit to chat room — clients with anti-delete will keep content
        io.to(`chat:${message.chat._id}`).emit('message:deleted', {
          messageId,
          chatId: message.chat._id.toString(),
          deletedFor: 'everyone'
        });
      } else {
        await Message.findByIdAndUpdate(messageId, { $addToSet: { deletedFor: userId } });
        socket.emit('message:deleted', { messageId, deletedFor: 'me' });
      }
    } catch (err) { console.error('message:delete error', err); }
  });

  // ── Typing indicators ────────────────────────────────────────────────────────
  socket.on('typing:start', ({ chatId }) => {
    socket.to(`chat:${chatId}`).emit('typing:start', { chatId, userId });
  });

  socket.on('typing:stop', ({ chatId }) => {
    socket.to(`chat:${chatId}`).emit('typing:stop', { chatId, userId });
  });

  // ── Reactions ────────────────────────────────────────────────────────────────
  socket.on('message:react', async ({ messageId, emoji, chatId }) => {
    try {
      const message = await Message.findById(messageId);
      if (!message) return;
      const idx = message.reactions.findIndex(r => r.userId.toString() === userId);
      if (idx >= 0) {
        message.reactions[idx].emoji === emoji
          ? message.reactions.splice(idx, 1)
          : (message.reactions[idx].emoji = emoji);
      } else {
        message.reactions.push({ userId, emoji });
      }
      await message.save();
      io.to(`chat:${chatId}`).emit('message:reactions_updated', {
        messageId, reactions: message.reactions
      });
    } catch (err) { console.error('react error', err); }
  });
}

module.exports = { messageHandler };
