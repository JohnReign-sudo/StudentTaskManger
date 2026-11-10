# Student Task Manager — Online AI Quiz Backend

This Node.js service receives a review module from the Android app, sends it to the OpenAI Responses API, and returns multiple-choice questions as JSON.

## Security
- The OpenAI API key belongs on the backend only. Do not put it in Kotlin, Android resources, GitHub, or the APK.
- Set `OPENAI_API_KEY` as a secret environment variable in the host dashboard.
- `.env` is ignored by Git. Never upload a real `.env` file.
- The sample server limits uploads and requests, but it is a starter project, not a production security review. Add user authentication and stronger abuse controls before public release.
- API usage can cost money. Set an appropriate usage budget/limit in your AI provider account and check usage regularly.

## Run locally
1. Install Node.js 20 or later.
2. In this folder, run `npm install`.
3. Copy `.env.example` to `.env` and put your own API key in `OPENAI_API_KEY`.
4. Run `npm start`.
5. Check `http://localhost:3000/health` in a browser.

The Android emulator uses `http://10.0.2.2:3000` to reach the host computer, but the app currently expects HTTPS for safety. For local emulator testing, change the HTTPS check in `QuizActivity.kt` to permit `http://10.0.2.2:3000`; do not use plain HTTP for public internet deployment.

## Deploy on Render
1. Create a Git repository containing this `backend` folder (or upload the full project to GitHub and set the service root directory to `backend`).
2. In Render, create **New → Web Service** and connect the repository.
3. Set Root Directory to `backend` if needed.
4. Build command: `npm install`
5. Start command: `npm start`
6. Add environment variables in Render's Environment section:
   - `OPENAI_API_KEY` = your secret API key
   - `OPENAI_MODEL` = `gpt-4.1-mini` (or a model available to your API account)
   - `MAX_UPLOAD_MB` = `10`
7. Deploy and open `https://YOUR-SERVICE.onrender.com/health`. It should return `{"ok":true,...}`.
8. Copy the HTTPS service URL into the Android app's backend URL field.

Free Render web services can sleep after inactivity and may have monthly usage limits, so the first quiz request can take longer. See Render's official free-service documentation.

## Supported files
PDF, DOCX, PPTX, TXT, MD, RTF, DOC, PPT, ODT. File contents are sent to the AI provider for quiz generation. Do not upload private, confidential, or sensitive documents.
