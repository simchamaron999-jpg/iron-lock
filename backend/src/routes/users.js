const express = require('express');
const router = express.Router();
const { User } = require('../models');
const { authMiddleware } = require('../middleware/auth');

router.use(authMiddleware);

// GET /api/users/me
router.get('/me', async (req, res, next) => {
  try {
    const user = await User.findById(req.userId).select('-fcmTokens -blockedUsers');
    res.json(user);
  } catch (err) { next(err); }
});

// PUT /api/users/me
router.put('/me', async (req, res, next) => {
  try {
    const { name, bio, avatarUrl, settings } = req.body;
    const update = {};
    if (name !== undefined) update.name = name;
    if (bio !== undefined) update.bio = bio;
    if (avatarUrl !== undefined) update.avatarUrl = avatarUrl;
    if (settings !== undefined) update.settings = settings;
    const user = await User.findByIdAndUpdate(req.userId, update, { new: true });
    res.json(user);
  } catch (err) { next(err); }
});

// POST /api/users/fcm-token — register device push token
router.post('/fcm-token', async (req, res, next) => {
  try {
    const { token } = req.body;
    if (!token) return res.status(400).json({ error: 'token required' });
    await User.findByIdAndUpdate(req.userId, { $addToSet: { fcmTokens: token } });
    res.json({ message: 'Token registered' });
  } catch (err) { next(err); }
});

// DELETE /api/users/fcm-token — remove device push token on logout
router.delete('/fcm-token', async (req, res, next) => {
  try {
    const { token } = req.body;
    await User.findByIdAndUpdate(req.userId, { $pull: { fcmTokens: token } });
    res.json({ message: 'Token removed' });
  } catch (err) { next(err); }
});

// POST /api/users/sync-contacts
// Receives array of phone numbers, returns registered users
router.post('/sync-contacts', async (req, res, next) => {
  try {
    const { phones } = req.body;
    if (!Array.isArray(phones)) return res.status(400).json({ error: 'phones array required' });
    const users = await User.find({ phone: { $in: phones }, _id: { $ne: req.userId } })
      .select('_id phone name avatarUrl bio lastSeen isOnline');
    res.json(users);
  } catch (err) { next(err); }
});

// GET /api/users/:id
router.get('/:id', async (req, res, next) => {
  try {
    const user = await User.findById(req.params.id).select('-fcmTokens');
    if (!user) return res.status(404).json({ error: 'User not found' });

    // Respect ghost mode and last seen privacy
    const result = user.toObject();
    if (user.ghostMode || !user.settings.showOnlineStatus) {
      result.isOnline = false;
    }
    if (!user.settings.showLastSeen) {
      result.lastSeen = null;
    } else if (user.frozenLastSeen) {
      result.lastSeen = user.frozenLastSeen;
    }

    res.json(result);
  } catch (err) { next(err); }
});

// POST /api/users/block/:id
router.post('/block/:id', async (req, res, next) => {
  try {
    await User.findByIdAndUpdate(req.userId, { $addToSet: { blockedUsers: req.params.id } });
    res.json({ message: 'Blocked' });
  } catch (err) { next(err); }
});

// DELETE /api/users/block/:id
router.delete('/block/:id', async (req, res, next) => {
  try {
    await User.findByIdAndUpdate(req.userId, { $pull: { blockedUsers: req.params.id } });
    res.json({ message: 'Unblocked' });
  } catch (err) { next(err); }
});

module.exports = router;
