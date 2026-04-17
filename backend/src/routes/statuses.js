const express = require('express');
const router = express.Router();
const { Status, User } = require('../models');
const { authMiddleware } = require('../middleware/auth');

router.use(authMiddleware);

// GET /api/statuses — statuses from contacts
router.get('/', async (req, res, next) => {
  try {
    const me = await User.findById(req.userId).select('blockedUsers');
    const statuses = await Status.find({
      expiresAt: { $gt: new Date() },
      user: { $nin: me.blockedUsers }
    }).populate('user', 'name avatarUrl').sort({ createdAt: -1 });
    res.json(statuses);
  } catch (err) { next(err); }
});

// POST /api/statuses
router.post('/', async (req, res, next) => {
  try {
    const { type, content, mediaUrl, bgColor, textColor, fontIndex } = req.body;
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24h
    const status = await Status.create({
      user: req.userId, type, content, mediaUrl, bgColor, textColor, fontIndex, expiresAt
    });
    res.status(201).json(status);
  } catch (err) { next(err); }
});

// POST /api/statuses/:id/view
router.post('/:id/view', async (req, res, next) => {
  try {
    await Status.findByIdAndUpdate(req.params.id, {
      $addToSet: { viewers: { user: req.userId, viewedAt: new Date() } }
    });
    res.json({ message: 'Viewed' });
  } catch (err) { next(err); }
});

// DELETE /api/statuses/:id
router.delete('/:id', async (req, res, next) => {
  try {
    const status = await Status.findById(req.params.id);
    if (status?.user.toString() !== req.userId) return res.status(403).json({ error: 'Forbidden' });
    await status.deleteOne();
    res.json({ message: 'Deleted' });
  } catch (err) { next(err); }
});

module.exports = router;
