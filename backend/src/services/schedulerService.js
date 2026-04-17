const { Queue, Worker } = require('bullmq');
const { Message, Chat } = require('../models');

let messageQueue;
let worker;

function getQueue() {
  if (!messageQueue) {
    messageQueue = new Queue('scheduled-messages', {
      connection: { url: process.env.REDIS_URL || 'redis://localhost:6379' }
    });
  }
  return messageQueue;
}

async function scheduleMessage(messageId, scheduledAt) {
  const delay = scheduledAt.getTime() - Date.now();
  if (delay < 0) return; // Already past — send immediately

  const queue = getQueue();
  const job = await queue.add('send', { messageId }, { delay, jobId: messageId });
  return job.id;
}

async function cancelScheduledMessage(messageId) {
  const queue = getQueue();
  const job = await queue.getJob(messageId);
  if (job) await job.remove();
}

function startWorker(io) {
  worker = new Worker(
    'scheduled-messages',
    async (job) => {
      const { messageId } = job.data;
      const message = await Message.findById(messageId).populate('chat');
      if (!message || message.isSent) return;

      message.isSent = true;
      message.scheduledAt = undefined;
      await message.save();

      // Emit to chat room via Socket.IO
      if (io) {
        io.to(`chat:${message.chat._id}`).emit('message:receive', {
          ...message.toObject(),
          chatId: message.chat._id.toString()
        });
      }
    },
    { connection: { url: process.env.REDIS_URL || 'redis://localhost:6379' } }
  );

  worker.on('failed', (job, err) => console.error(`Scheduled message failed ${job?.id}:`, err));
}

module.exports = { scheduleMessage, cancelScheduledMessage, startWorker };
