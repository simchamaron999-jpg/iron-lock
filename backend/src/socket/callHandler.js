const { Call, Chat, User } = require('../models');
const { sendPushToUsers } = require('../services/pushService');

// Phase 6: mediasoup SFU rooms created here for group calls
function callHandler(io, socket) {
  const userId = socket.userId;

  socket.on('call:initiate', async ({ chatId, type }) => {
    try {
      const chat = await Chat.findById(chatId).populate('members', '_id fcmTokens name');
      if (!chat?.members.find(m => m._id.toString() === userId)) return;

      const call = await Call.create({
        chat: chatId,
        initiator: userId,
        participants: [userId],
        type,
        status: 'ringing'
      });

      const caller = await User.findById(userId).select('name avatarUrl');
      const callData = { callId: call._id.toString(), chatId, type, caller: { id: userId, name: caller.name, avatarUrl: caller.avatarUrl } };

      // Ring all other members
      chat.members.forEach(member => {
        if (member._id.toString() !== userId) {
          io.to(`user:${member._id}`).emit('call:incoming', callData);
        }
      });

      // Push notification for offline members
      const offlineFcmTokens = chat.members
        .filter(m => m._id.toString() !== userId && !io.sockets.adapter.rooms.has(`user:${m._id}`))
        .flatMap(m => m.fcmTokens || []);

      if (offlineFcmTokens.length > 0) {
        await sendPushToUsers(offlineFcmTokens, {
          type: 'call',
          callId: call._id.toString(),
          callerName: caller.name,
          callType: type
        });
      }

      socket.emit('call:initiated', callData);
    } catch (err) { console.error('call:initiate error', err); }
  });

  socket.on('call:answer', async ({ callId }) => {
    try {
      const call = await Call.findByIdAndUpdate(callId, { status: 'ongoing', startedAt: new Date() }, { new: true });
      socket.to(`user:${call.initiator}`).emit('call:answered', { callId });
    } catch (err) { console.error('call:answer error', err); }
  });

  socket.on('call:reject', async ({ callId }) => {
    try {
      const call = await Call.findByIdAndUpdate(callId, { status: 'missed', endedAt: new Date() }, { new: true });
      socket.to(`user:${call.initiator}`).emit('call:rejected', { callId, userId });
    } catch (err) { console.error('call:reject error', err); }
  });

  socket.on('call:end', async ({ callId }) => {
    try {
      const call = await Call.findById(callId);
      if (!call) return;
      const durationSeconds = call.startedAt ? Math.floor((Date.now() - call.startedAt) / 1000) : 0;
      await Call.findByIdAndUpdate(callId, { status: 'ended', endedAt: new Date(), durationSeconds });

      // Notify all participants
      call.participants.forEach(participantId => {
        io.to(`user:${participantId}`).emit('call:ended', { callId, durationSeconds });
      });
    } catch (err) { console.error('call:end error', err); }
  });

  // WebRTC signaling relay
  socket.on('call:offer', ({ callId, targetUserId, sdp }) => {
    io.to(`user:${targetUserId}`).emit('call:offer', { callId, sdp, fromUserId: userId });
  });

  socket.on('call:answer_sdp', ({ callId, targetUserId, sdp }) => {
    io.to(`user:${targetUserId}`).emit('call:answer_sdp', { callId, sdp });
  });

  socket.on('call:ice', ({ callId, targetUserId, candidate }) => {
    io.to(`user:${targetUserId}`).emit('call:ice', { callId, candidate });
  });
}

module.exports = { callHandler };
