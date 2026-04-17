const mongoose = require('mongoose');
const { Schema } = mongoose;

// ─── User ───────────────────────────��──────────────────────���─────────────────
const userSchema = new Schema({
  phone: { type: String, required: true, unique: true },
  name: { type: String, default: '' },
  avatarUrl: { type: String, default: '' },
  bio: { type: String, default: '' },
  lastSeen: { type: Date, default: Date.now },
  isOnline: { type: Boolean, default: false },
  frozenLastSeen: { type: Date, default: null }, // freeze last seen feature
  ghostMode: { type: Boolean, default: false },
  fcmTokens: [String],
  settings: {
    showLastSeen: { type: Boolean, default: true },
    showOnlineStatus: { type: Boolean, default: true },
    showReadReceipts: { type: Boolean, default: true },
    showTyping: { type: Boolean, default: true }
  },
  blockedUsers: [{ type: Schema.Types.ObjectId, ref: 'User' }]
}, { timestamps: true });

// ─── Chat ─────────────────────────────────────────────────────────────────���──
const chatSchema = new Schema({
  type: { type: String, enum: ['dm', 'group'], required: true },
  members: [{ type: Schema.Types.ObjectId, ref: 'User' }],
  lastMessage: { type: Schema.Types.ObjectId, ref: 'Message' },
  // Group-only fields
  name: { type: String },
  avatarUrl: { type: String },
  admins: [{ type: Schema.Types.ObjectId, ref: 'User' }],
  description: { type: String },
  inviteCode: { type: String },
  memberLimit: { type: Number, default: 1024 }
}, { timestamps: true });

chatSchema.index({ members: 1 });

// ─── Message ─────────────────────────────────────────────────────────────────
const messageSchema = new Schema({
  chat: { type: Schema.Types.ObjectId, ref: 'Chat', required: true },
  sender: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  type: {
    type: String,
    enum: ['text', 'image', 'video', 'audio', 'document', 'gif', 'sticker', 'location', 'contact'],
    default: 'text'
  },
  content: { type: String, default: '' },
  mediaUrl: { type: String },
  mediaMimeType: { type: String },
  mediaSize: { type: Number },
  thumbnailUrl: { type: String },
  duration: { type: Number }, // audio/video duration in seconds
  // Location
  latitude: Number,
  longitude: Number,
  // Link preview
  linkPreview: {
    title: String,
    description: String,
    imageUrl: String,
    url: String
  },
  // Threading
  replyTo: { type: Schema.Types.ObjectId, ref: 'Message' },
  isForwarded: { type: Boolean, default: false },
  // Delivery
  deliveredTo: [{ type: Schema.Types.ObjectId, ref: 'User' }],
  readBy: [{ type: Schema.Types.ObjectId, ref: 'User' }],
  // Reactions: [{ userId, emoji }]
  reactions: [{ userId: Schema.Types.ObjectId, emoji: String }],
  // Deletion
  deletedFor: [{ type: Schema.Types.ObjectId, ref: 'User' }],
  deletedForEveryone: { type: Boolean, default: false },
  revokedAt: { type: Date },
  // Scheduling
  scheduledAt: { type: Date },
  isSent: { type: Boolean, default: true }
}, { timestamps: true });

messageSchema.index({ chat: 1, createdAt: -1 });
messageSchema.index({ content: 'text' }); // full-text search

// ─── Status ──────────────────────────────────────────────────────────────────
const statusSchema = new Schema({
  user: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  type: { type: String, enum: ['text', 'image', 'video'], required: true },
  content: { type: String, default: '' },
  mediaUrl: { type: String },
  bgColor: { type: String, default: '#000000' },
  textColor: { type: String, default: '#FFFFFF' },
  fontIndex: { type: Number, default: 0 },
  viewers: [{ user: Schema.Types.ObjectId, viewedAt: Date }],
  expiresAt: { type: Date, required: true }
}, { timestamps: true });

statusSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 }); // auto-TTL

// ─── Call ─────────────────────────────────────────────────────────────────��──
const callSchema = new Schema({
  chat: { type: Schema.Types.ObjectId, ref: 'Chat', required: true },
  initiator: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  participants: [{ type: Schema.Types.ObjectId, ref: 'User' }],
  type: { type: String, enum: ['voice', 'video'], required: true },
  status: { type: String, enum: ['ringing', 'ongoing', 'ended', 'missed'], default: 'ringing' },
  mediasoupRoomId: { type: String },
  startedAt: Date,
  endedAt: Date,
  durationSeconds: Number
}, { timestamps: true });

// ─── Scheduled Message ───────────────────────────────────────────────────────
const scheduledMessageSchema = new Schema({
  chat: { type: Schema.Types.ObjectId, ref: 'Chat', required: true },
  sender: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  content: { type: String, default: '' },
  type: { type: String, default: 'text' },
  mediaUrl: { type: String },
  scheduledAt: { type: Date, required: true },
  status: { type: String, enum: ['pending', 'sent', 'cancelled'], default: 'pending' },
  bullJobId: { type: String }
}, { timestamps: true });

// ─── Auto Reply Rule ─────────────────────────────────────────────────────��───
const autoReplySchema = new Schema({
  user: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  trigger: { type: String, required: true },
  response: { type: String, required: true },
  scope: { type: String, enum: ['all', 'contact', 'group'], default: 'all' },
  scopeId: { type: Schema.Types.ObjectId },
  isEnabled: { type: Boolean, default: true }
}, { timestamps: true });

module.exports = {
  User: mongoose.model('User', userSchema),
  Chat: mongoose.model('Chat', chatSchema),
  Message: mongoose.model('Message', messageSchema),
  Status: mongoose.model('Status', statusSchema),
  Call: mongoose.model('Call', callSchema),
  ScheduledMessage: mongoose.model('ScheduledMessage', scheduledMessageSchema),
  AutoReply: mongoose.model('AutoReply', autoReplySchema)
};
