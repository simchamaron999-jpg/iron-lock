const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const twilio = require('twilio');
const { User } = require('../models');
const { authMiddleware } = require('../middleware/auth');

const twilioClient = twilio(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN);

function issueTokens(userId) {
  const accessToken = jwt.sign({ userId }, process.env.JWT_ACCESS_SECRET, {
    expiresIn: process.env.JWT_ACCESS_EXPIRES || '15m'
  });
  const refreshToken = jwt.sign({ userId }, process.env.JWT_REFRESH_SECRET, {
    expiresIn: process.env.JWT_REFRESH_EXPIRES || '30d'
  });
  return { accessToken, refreshToken };
}

// POST /api/auth/send-otp
router.post('/send-otp', async (req, res, next) => {
  try {
    const { phone } = req.body;
    if (!phone) return res.status(400).json({ error: 'Phone required' });

    await twilioClient.verify.v2
      .services(process.env.TWILIO_VERIFY_SERVICE_SID)
      .verifications.create({ to: phone, channel: 'sms' });

    res.json({ message: 'OTP sent' });
  } catch (err) { next(err); }
});

// POST /api/auth/verify-otp
router.post('/verify-otp', async (req, res, next) => {
  try {
    const { phone, otp } = req.body;
    if (!phone || !otp) return res.status(400).json({ error: 'Phone and OTP required' });

    const check = await twilioClient.verify.v2
      .services(process.env.TWILIO_VERIFY_SERVICE_SID)
      .verificationChecks.create({ to: phone, code: otp });

    if (check.status !== 'approved') {
      return res.status(401).json({ error: 'Invalid OTP' });
    }

    let user = await User.findOne({ phone });
    if (!user) user = await User.create({ phone });

    const tokens = issueTokens(user._id.toString());
    res.json({ ...tokens, userId: user._id.toString() });
  } catch (err) { next(err); }
});

// POST /api/auth/profile
router.post('/profile', authMiddleware, async (req, res, next) => {
  try {
    const { name, bio } = req.body;
    await User.findByIdAndUpdate(req.userId, { name, bio });
    res.json({ message: 'Profile saved' });
  } catch (err) { next(err); }
});

// POST /api/auth/refresh
router.post('/refresh', async (req, res, next) => {
  try {
    const { refreshToken } = req.body;
    const payload = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET);
    const tokens = issueTokens(payload.userId);
    res.json(tokens);
  } catch {
    res.status(401).json({ error: 'Invalid refresh token' });
  }
});

// POST /api/auth/fcm-token
router.post('/fcm-token', authMiddleware, async (req, res, next) => {
  try {
    const { token } = req.body;
    await User.findByIdAndUpdate(req.userId, { $addToSet: { fcmTokens: token } });
    res.json({ message: 'FCM token saved' });
  } catch (err) { next(err); }
});

module.exports = router;
