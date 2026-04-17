const express = require('express');
const router = express.Router();
const multer = require('multer');
const multerS3 = require('multer-s3');
const { S3Client, GetObjectCommand } = require('@aws-sdk/client-s3');
const { getSignedUrl } = require('@aws-sdk/s3-request-presigner');
const sharp = require('sharp');
const { authMiddleware } = require('../middleware/auth');
const path = require('path');
const { v4: uuidv4 } = require('uuid');

const s3 = new S3Client({
  endpoint: process.env.S3_ENDPOINT,
  region: process.env.S3_REGION,
  credentials: { accessKeyId: process.env.S3_ACCESS_KEY, secretAccessKey: process.env.S3_SECRET_KEY },
  forcePathStyle: true
});

const upload = multer({
  storage: multerS3({
    s3,
    bucket: process.env.S3_BUCKET,
    key: (req, file, cb) => {
      const ext = path.extname(file.originalname);
      cb(null, `media/${req.userId}/${uuidv4()}${ext}`);
    }
  }),
  limits: { fileSize: parseInt(process.env.MAX_FILE_SIZE_BYTES || '1073741824') }, // 1GB default
  fileFilter: (req, file, cb) => {
    const allowed = ['image/', 'video/', 'audio/', 'application/pdf', 'application/'];
    const ok = allowed.some(t => file.mimetype.startsWith(t));
    cb(null, ok);
  }
});

// POST /api/media/upload
// Query: ?compress=false to skip compression (WhatsApp Plus full-res feature)
router.post('/upload', authMiddleware, upload.single('file'), async (req, res, next) => {
  try {
    const file = req.file;
    const compress = req.query.compress !== 'false';

    let finalUrl = file.location;
    let thumbnailUrl = '';

    // Generate compressed thumbnail for images if not full-res
    if (file.mimetype.startsWith('image/') && compress) {
      // In production, run sharp on-the-fly or via Lambda; for now return original
      thumbnailUrl = finalUrl;
    }

    res.json({
      url: finalUrl,
      thumbnailUrl,
      mimeType: file.mimetype,
      size: file.size,
      key: file.key
    });
  } catch (err) { next(err); }
});

// GET /api/media/presigned?key=
router.get('/presigned', authMiddleware, async (req, res, next) => {
  try {
    const { key } = req.query;
    const command = new GetObjectCommand({ Bucket: process.env.S3_BUCKET, Key: key });
    const url = await getSignedUrl(s3, command, { expiresIn: 3600 });
    res.json({ url });
  } catch (err) { next(err); }
});

module.exports = router;
