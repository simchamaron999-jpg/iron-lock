const express = require('express');
const router = express.Router();
const { Call } = require('../models');
const { authMiddleware } = require('../middleware/auth');

router.use(authMiddleware);

// GET /api/calls — call log
router.get('/', async (req, res, next) => {
  try {
    const calls = await Call.find({ participants: req.userId })
      .populate('participants', 'name avatarUrl')
      .populate('initiator', 'name')
      .sort({ createdAt: -1 })
      .limit(100);
    res.json(calls);
  } catch (err) { next(err); }
});

// POST /api/calls — initiate call (REST, Socket.IO also triggers)
router.post('/', async (req, res, next) => {
  try {
    const { chatId, type } = req.body;
    const call = await Call.create({
      chat: chatId,
      initiator: req.userId,
      participants: [req.userId],
      type
    });
    res.status(201).json(call);
  } catch (err) { next(err); }
});

module.exports = router;
