import express from "express";
import multer from "multer";
import OpenAI from "openai";
import rateLimit from "express-rate-limit";
import { fileTypeFromBuffer } from "file-type";

const app = express();
const port = Number(process.env.PORT || 3000);
const maxMb = Math.min(Math.max(Number(process.env.MAX_UPLOAD_MB || 10), 1), 20);
const allowedExtensions = new Set(["pdf", "docx", "pptx", "txt", "md", "rtf", "doc", "ppt", "odt"]);
const allowedMimes = new Set([
  "application/pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  "text/plain", "text/markdown", "application/rtf", "application/msword",
  "application/vnd.ms-powerpoint", "application/vnd.oasis.opendocument.text"
]);

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: maxMb * 1024 * 1024, files: 1 }
});
const limiter = rateLimit({ windowMs: 60 * 1000, limit: 8, standardHeaders: "draft-7", legacyHeaders: false });

app.disable("x-powered-by");
app.get("/health", (_req, res) => res.json({ ok: true, service: "student-task-manager-quiz-backend" }));

const quizSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    questions: {
      type: "array",
      minItems: 5,
      maxItems: 10,
      items: {
        type: "object",
        additionalProperties: false,
        properties: {
          question: { type: "string" },
          options: { type: "array", items: { type: "string" }, minItems: 4, maxItems: 4 },
          answer: { type: "string", enum: ["A", "B", "C", "D"] },
          explanation: { type: "string" }
        },
        required: ["question", "options", "answer", "explanation"]
      }
    }
  },
  required: ["questions"]
};

app.post("/api/quiz", limiter, upload.single("file"), async (req, res) => {
  try {
    if (!process.env.OPENAI_API_KEY) {
      return res.status(503).json({ error: "The server administrator has not configured OPENAI_API_KEY." });
    }
    if (!req.file) return res.status(400).json({ error: "Upload one review file using the field name 'file'." });

    const filename = (req.file.originalname || "review-file").replace(/[\r\n"\\]/g, "_");
    const extension = filename.split(".").pop()?.toLowerCase() || "";
    const detected = await fileTypeFromBuffer(req.file.buffer).catch(() => undefined);
    const mime = req.file.mimetype || detected?.mime || "application/octet-stream";
    const isText = ["txt", "md", "rtf"].includes(extension) && req.file.buffer.length > 0;
    if (!allowedExtensions.has(extension) || (!allowedMimes.has(mime) && !isText && !detected)) {
      return res.status(415).json({ error: "Unsupported file. Use PDF, DOCX, PPTX, TXT, MD, RTF, DOC, PPT, or ODT." });
    }
    if (req.file.buffer.length === 0) return res.status(400).json({ error: "The uploaded file is empty." });

    const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
    const base64 = req.file.buffer.toString("base64");
    const extensionMime = {
      pdf: "application/pdf",
      docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      pptx: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      txt: "text/plain", md: "text/markdown", rtf: "application/rtf",
      doc: "application/msword", ppt: "application/vnd.ms-powerpoint",
      odt: "application/vnd.oasis.opendocument.text"
    };
    const dataMime = extensionMime[extension] || (allowedMimes.has(mime) ? mime : "text/plain");
    const response = await client.responses.create({
      model: process.env.OPENAI_MODEL || "gpt-4.1-mini",
      input: [{
        role: "user",
        content: [
          {
            type: "input_file",
            filename,
            file_data: `data:${dataMime};base64,${base64}`
          },
          {
            type: "input_text",
            text: "Create 5 to 10 clear practice multiple-choice questions based only on the uploaded review module. Each question must have exactly four plausible options. Include one correct answer letter A, B, C, or D and a short explanation grounded in the module. Avoid questions whose answers are not supported by the file. Output the requested JSON structure only."
          }
        ]
      }],
      text: {
        format: {
          type: "json_schema",
          name: "study_quiz",
          strict: true,
          schema: quizSchema
        }
      }
    });

    if (!response.output_text) return res.status(502).json({ error: "The AI did not return quiz questions. Try another review file." });
    const parsed = JSON.parse(response.output_text);
    return res.json(parsed);
  } catch (error) {
    console.error("Quiz generation failed:", error?.status || "unknown", error?.message || "unknown error");
    const status = error?.status === 429 ? 429 : 502;
    return res.status(status).json({
      error: error?.status === 429
        ? "AI rate limit or usage limit reached. Wait a little and check your API account usage."
        : "Quiz generation failed. Check the server logs, API key, supported file type, and AI account access."
    });
  }
});

app.use((err, _req, res, _next) => {
  if (err instanceof multer.MulterError && err.code === "LIMIT_FILE_SIZE") {
    return res.status(413).json({ error: `File too large. Maximum upload size is ${maxMb} MB.` });
  }
  console.error("Request error:", err?.message || "unknown error");
  return res.status(400).json({ error: "The request could not be processed." });
});

app.listen(port, "0.0.0.0", () => console.log(`Quiz backend listening on port ${port}`));
