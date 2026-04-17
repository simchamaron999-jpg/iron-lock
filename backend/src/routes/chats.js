const express = require('express');
const router = express.Router();
const { Chat, Message } = require('../models');
const { authMiddleware } = require('../middleware/auth');
const { v4: uuidv4 } = require('uuid');

router.use(authMiddleware);

// GET /api/chats — list all chats for current user
router.get('/', async (req, res, next) => {
  try {
    const chats = await Chat.find({ members: req.userId })
      .populate('members', 'name avatarUrl isOnline lastSeen frozenLastSeen ghostMode settings')
      .populate({ path: 'lastMessage', populate: { path: 'sender', select: 'name' } })
      .sort({ updatedAt: -1 });
    res.json(chats);
  } catch (err) { next(err); }
});

// POST /api/chats/dm — create or get existing DM
router.post('/dm', async (req, res, next) => {
  try {
    const { userId } = req.body;
    let chat = await Chat.findOne({
      type: 'dm',
      members: { $all: [req.userId, userId], $size: 2 }
    }).populate('members', 'name avatarUrl');

    if (!chat) {
      chat = await Chat.create({ type: 'dm', members: [req.userId, userId] });
      chat = await chat.populate('members', 'name avatarUrl');
    }

    res.json(chat);
  } catch (err) { next(err); }
});

// POST /api/chats/group — create group
router.post('/group', async (req, res, next) => {
  try {
    const { name, memberIds, avatarUrl } = req.body;
    const members = [...new Set([req.userId, ...memberIds])];
    const chat = await Chat.create({
      type: 'group',
      name,
      avatarUrl,
      members,
      admins: [req.userId],
      inviteCode: uuidv4().slice(0, 8)
    });
    res.status(201).json(chat);
  } catch (err) { next(err); }
});

// GET /api/chats/:id
router.get('/:id', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id)
      .populate('members', 'name avatarUrl isOnline lastSeen frozenLastSeen ghostMode settings')
      .populate('admins', 'name');
    if (!chat?.members.find(m => m._id.toString() === req.userId)) return res.status(403).json({ error: 'Forbidden' });
    res.json(chat);
  } catch (err) { next(err); }
});

// PUT /api/chats/:id — update group info (admins only)
router.put('/:id', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    if (!chat?.admins.includes(req.userId)) return res.status(403).json({ error: 'Admins only' });
    const { name, avatarUrl, description } = req.body;
    const update = {};
    if (name) update.name = name;
    if (avatarUrl) update.avatarUrl = avatarUrl;
    if (description) update.description = description;
    const updated = await Chat.findByIdAndUpdate(req.params.id, update, { new: true });
    res.json(updated);
  } catch (err) { next(err); }
});

// GET /api/chats/:chatId/group — full group info
router.get('/:id/group', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id)
      .populate('members', 'name avatarUrl')
      .populate('admins', '_id');
    if (!chat?.members.find(m => m._id.toString() === req.userId)) return res.status(403).json({ error: 'Forbidden' });
    res.json({
      _id: chat._id,
      name: chat.name,
      avatarUrl: chat.avatarUrl,
      description: chat.description,
      memberIds: chat.members.map(m => m._id.toString()),
      adminIds: chat.admins.map(a => a._id.toString()),
      inviteLink: chat.inviteCode ? `https://mistymessenger.app/join/${chat.inviteCode}` : null
    });
  } catch (err) { next(err); }
});

// PATCH /api/chats/:chatId/group — update group info
router.patch('/:id/group', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    if (!chat?.admins.map(a => a.toString()).includes(req.userId)) return res.status(403).json({ error: 'Admins only' });
    const { name, description, avatarUrl } = req.body;
    const update = {};
    if (name) update.name = name;
    if (description !== undefined) update.description = description;
    if (avatarUrl) update.avatarUrl = avatarUrl;
    const updated = await Chat.findByIdAndUpdate(req.params.id, update, { new: true });
    res.json({ _id: updated._id, name: updated.name, avatarUrl: updated.avatarUrl,
      description: updated.description, memberIds: updated.members, adminIds: updated.admins });
  } catch (err) { next(err); }
});

// POST /api/chats/:chatId/group/members — add member
router.post('/:id/group/members', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    if (!chat?.admins.map(a => a.toString()).includes(req.userId)) return res.status(403).json({ error: 'Admins only' });
    const updated = await Chat.findByIdAndUpdate(
      req.params.id,
      { $addToSet: { members: req.body.userId } },
      { new: true }
    );
    res.json({ _id: updated._id, memberIds: updated.members, adminIds: updated.admins });
  } catch (err) { next(err); }
});

// DELETE /api/chats/:chatId/group/members/:userId
router.delete('/:id/group/members/:userId', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    const isSelf = req.params.userId === req.userId;
    const isAdmin = chat?.admins.map(a => a.toString()).includes(req.userId);
    if (!isSelf && !isAdmin) return res.status(403).json({ error: 'Forbidden' });
    const updated = await Chat.findByIdAndUpdate(
      req.params.id,
      { $pull: { members: req.params.userId, admins: req.params.userId } },
      { new: true }
    );
    res.json({ _id: updated._id, memberIds: updated.members, adminIds: updated.admins });
  } catch (err) { next(err); }
});

// POST /api/chats/:chatId/group/admins — promote to admin
router.post('/:id/group/admins', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    if (!chat?.admins.map(a => a.toString()).includes(req.userId)) return res.status(403).json({ error: 'Admins only' });
    const updated = await Chat.findByIdAndUpdate(
      req.params.id,
      { $addToSet: { admins: req.body.userId } },
      { new: true }
    );
    res.json({ _id: updated._id, memberIds: updated.members, adminIds: updated.admins });
  } catch (err) { next(err); }
});

// DELETE /api/chats/:chatId/group/admins/:userId — demote from admin
router.delete('/:id/group/admins/:userId', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    if (!chat?.admins.map(a => a.toString()).includes(req.userId)) return res.status(403).json({ error: 'Admins only' });
    const updated = await Chat.findByIdAndUpdate(
      req.params.id,
      { $pull: { admins: req.params.userId } },
      { new: true }
    );
    res.json({ _id: updated._id, memberIds: updated.members, adminIds: updated.admins });
  } catch (err) { next(err); }
});

// POST /api/chats/:chatId/group/invite — generate invite link
router.post('/:id/group/invite', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    if (!chat?.admins.map(a => a.toString()).includes(req.userId)) return res.status(403).json({ error: 'Admins only' });
    const code = uuidv4().slice(0, 12);
    await Chat.findByIdAndUpdate(req.params.id, { inviteCode: code });
    res.json({ link: `https://mistymessenger.app/join/${code}` });
  } catch (err) { next(err); }
});

// DELETE /api/chats/:chatId/group/leave
router.delete('/:id/group/leave', async (req, res, next) => {
  try {
    await Chat.findByIdAndUpdate(req.params.id, { $pull: { members: req.userId, admins: req.userId } });
    res.json({ message: 'Left group' });
  } catch (err) { next(err); }
});

// POST /api/chats/:id/members (legacy)
router.post('/:id/members', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    if (!chat?.admins.includes(req.userId)) return res.status(403).json({ error: 'Admins only' });
    await Chat.findByIdAndUpdate(req.params.id, { $addToSet: { members: { $each: req.body.userIds } } });
    res.json({ message: 'Members added' });
  } catch (err) { next(err); }
});

// DELETE /api/chats/:id/members/:userId (legacy)
router.delete('/:id/members/:userId', async (req, res, next) => {
  try {
    const chat = await Chat.findById(req.params.id);
    const isSelf = req.params.userId === req.userId;
    const isAdmin = chat?.admins.map(a => a.toString()).includes(req.userId);
    if (!isSelf && !isAdmin) return res.status(403).json({ error: 'Forbidden' });
    await Chat.findByIdAndUpdate(req.params.id, { $pull: { members: req.params.userId, admins: req.params.userId } });
    res.json({ message: 'Removed' });
  } catch (err) { next(err); }
});

module.exports = router;
