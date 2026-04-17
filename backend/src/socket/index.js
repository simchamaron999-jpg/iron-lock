const { socketAuthMiddleware } = require('../middleware/auth');
const { messageHandler } = require('./messageHandler');
const { presenceHandler } = require('./presenceHandler');
const { callHandler } = require('./callHandler');

function initSocket(io) {
  // Auth gate for all socket connections
  io.use(socketAuthMiddleware);

  io.on('connection', (socket) => {
    console.log(`Socket connected: ${socket.id} user: ${socket.userId}`);

    // Join personal room for direct notifications
    socket.join(`user:${socket.userId}`);

    // Register feature handlers
    messageHandler(io, socket);
    presenceHandler(io, socket);
    callHandler(io, socket);

    socket.on('disconnect', (reason) => {
      console.log(`Socket disconnected: ${socket.id} reason: ${reason}`);
    });
  });
}

module.exports = { initSocket };
