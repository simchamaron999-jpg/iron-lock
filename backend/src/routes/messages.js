const express = require('express');
const router = express.Router();
const { Message, Chat, ScheduledMessage } = require('../models');
const { authMiddleware } = require('../middleware/auth');
const { scheduleMessage } = require('../services/schedulerService');

router.use(authMiddleware);

// GET /api/messages/:chatId?cursor=&limit=30
router.get('/:chatId', async (req, res, next) => {
  try {
    const { cursor, limit = 30 } = req.query;
    const chat = await Chat.findById(req.params.chatId);
    if (!chat?.members.includes(req.userId)) return res.status(403).json({ error: 'Forbidden' });

    const query = { chat: req.params.chatId, deletedFor: { $ne: req.userId } };
    if (cursor) query._id = { $lt: cursor };

    const messages = await Message.find(query)
      .sort({ _id: -1 })
      .limit(parseInt(limit))
      .populate('sender', 'name avatarUrl')
      .populate('replyTo', 'content type sender');

    res.json(messages);
  } catch (err) { next(err); }
});

// POST /api/messages/:chatId (REST fallback, primary path is Socket.IO)
router.post('/:chatId', async (req, res, next) => {
  try {
    const { content, type, mediaUrl, replyTo, scheduledAt } = req.body;
    const message = await Message.create({
      chat: req.params.chatId,
      sender: req.userId,
      type: type || 'text',
      content,
      mediaUrl,
      replyTo,
      scheduledAt: scheduledAt ? new Date(scheduledAt) : undefined,
      isSent: !scheduledAt
    });

    if (scheduledAt) {
      await scheduleMessage(message._id.toString(), new Date(scheduledAt));
    }

    res.status(201).json(message);
  } catch (err) { next(err); }
});

// DELETE /api/messages/:id (delete for me)
router.delete('/:id', async (req, res, next) => {
  try {
    await Message.findByIdAndUpdate(req.params.id, { $addToSet: { deletedFor: req.userId } });
    res.json({ message: 'Deleted for you' });
  } catch (err) { next(err); }
});

// POST /api/messages/:id/revoke (delete for everyone)
router.post('/:id/revoke', async (req, res, next) => {
  try {
    const message = await Message.findById(req.params.id);
    if (!message) return res.status(404).json({ error: 'Not found' });
    if (message.sender.toString() !== req.userId) return res.status(403).json({ error: 'Forbidden' });

    const windowDays = parseInt(process.env.REVOKE_WINDOW_DAYS || '7');
    const windowMs = windowDays * 24 * 60 * 60 * 1000;
    if (Date.now() - message.createdAt > windowMs) {
      return res.status(400).json({ error: 'Revoke window expired' });
    }

    message.deletedForEveryone = true;
    message.content = '';
    message.mediaUrl = undefined;
    message.revokedAt = new Date();
    await message.save();

    res.json({ message: 'Deleted for everyone' });
  } catch (err) { next(err); }
});

// POST /api/messages/:id/react
router.post('/:id/react', async (req, res, next) => {
  try {
    const { emoji } = req.body;
    const message = await Message.findById(req.params.id);
    if (!message) return res.status(404).json({ error: 'Not found' });

    const existing = message.reactions.findIndex(r => r.userId.toString() === req.userId);
    if (existing >= 0) {
      if (message.reactions[existing].emoji === emoji) {
        message.reactions.splice(existing, 1); // toggle off
      } else {
        message.reactions[existing].emoji = emoji;
      }
    } else {
      message.reactions.push({ userId: req.userId, emoji });
    }
    await message.save();
    res.json(message.reactions);
  } catch (err) { next(err); }
});

// POST /api/messages/:id/star
router.post('/:id/star', async (req, res, next) => {
  try {
    // Phase 9: client-side starred messages are stored in Room.
    // Server just acknowledges for cross-device sync (future).
    res.json({ starred: true });
  } catch (err) { next(err); }
});

// GET /api/messages/search?q=&chatId=
router.get('/search', async (req, res, next) => {
  try {
    const { q, chatId } = req.query;
    const query = { $text: { $search: q }, deletedFor: { $ne: req.userId }, deletedForEveryone: false };
    if (chatId) query.chat = chatId;
    const results = await Message.find(query).limit(50).populate('sender', 'name');
    res.json(results);
  } catch (err) { next(err); }
});

module.exports = router;
