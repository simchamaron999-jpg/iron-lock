const { User, Chat } = require('../models');

// Tracks socket count per user for accurate online/offline
const userSocketCount = new Map();

function presenceHandler(io, socket) {
  const userId = socket.userId;

  async function broadcastPresence(isOnline) {
    try {
      const user = await User.findById(userId).select('ghostMode settings frozenLastSeen');
      if (!user) return;

      // Ghost mode: never broadcast as online
      if (user.ghostMode && isOnline) return;

      const lastSeen = isOnline ? undefined : (user.frozenLastSeen || new Date());

      await User.findByIdAndUpdate(userId, {
        isOnline: isOnline && !user.ghostMode,
        ...(lastSeen ? { lastSeen } : {})
      });

      // Notify mutual contacts who are online
      const chats = await Chat.find({ members: userId }).select('members');
      const contactIds = new Set();
      chats.forEach(chat => chat.members.forEach(m => {
        if (m.toString() !== userId) contactIds.add(m.toString());
      }));

      contactIds.forEach(contactId => {
        io.to(`user:${contactId}`).emit('user:presence', {
          userId,
          isOnline: isOnline && !user.ghostMode,
          lastSeen: user.settings.showLastSeen ? lastSeen?.toISOString() : null
        });
      });
    } catch (err) { console.error('presence broadcast error', err); }
  }

  // Track socket count
  const count = (userSocketCount.get(userId) || 0) + 1;
  userSocketCount.set(userId, count);
  if (count === 1) broadcastPresence(true);

  socket.on('disconnect', async () => {
    const newCount = (userSocketCount.get(userId) || 1) - 1;
    userSocketCount.set(userId, newCount);
    if (newCount <= 0) {
      userSocketCount.delete(userId);
      await broadcastPresence(false);
    }
  });

  // Heartbeat — client pings every 30s to keep lastSeen fresh
  socket.on('heartbeat', async () => {
    const user = await User.findById(userId).select('frozenLastSeen ghostMode');
    if (!user?.frozenLastSeen && !user?.ghostMode) {
      await User.findByIdAndUpdate(userId, { lastSeen: new Date() });
    }
  });
}

module.exports = { presenceHandler };
