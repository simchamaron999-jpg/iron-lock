require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const mongoose = require('mongoose');

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const chatRoutes = require('./routes/chats');
const messageRoutes = require('./routes/messages');
const mediaRoutes = require('./routes/media');
const statusRoutes = require('./routes/statuses');
const callRoutes = require('./routes/calls');

const { initSocket } = require('./socket');
const { errorHandler } = require('./middleware/errorHandler');

const app = express();
const server = http.createServer(app);

// Socket.IO
const io = new Server(server, {
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
    methods: ['GET', 'POST']
  },
  maxHttpBufferSize: 1e8 // 100MB for media over socket
});
initSocket(io);

// Security & parsing
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({ origin: process.env.ALLOWED_ORIGINS?.split(',') || '*', credentials: true }));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));

// Rate limiting
app.use('/api/auth', rateLimit({ windowMs: 15 * 60 * 1000, max: 20, message: 'Too many auth requests' }));
app.use('/api', rateLimit({ windowMs: 60 * 1000, max: 300 }));

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/chats', chatRoutes);
app.use('/api/messages', messageRoutes);
app.use('/api/media', mediaRoutes);
app.use('/api/statuses', statusRoutes);
app.use('/api/calls', callRoutes);

app.get('/health', (req, res) => res.json({ status: 'ok', uptime: process.uptime() }));

// Error handler
app.use(errorHandler);

// MongoDB connect + server start
async function start() {
  await mongoose.connect(process.env.MONGODB_URI, { dbName: 'mistymessenger' });
  console.log('MongoDB connected');

  const port = process.env.PORT || 3000;
  server.listen(port, () => console.log(`MistyMessenger backend running on :${port}`));
}

start().catch(err => { console.error('Startup error:', err); process.exit(1); });

module.exports = { app, io };
