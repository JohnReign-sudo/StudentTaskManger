# Student Task Manager — Android + Online AI Quiz

This project contains:
- `android/` — Android Studio Kotlin app source.
- `backend/` — Node.js API that securely calls OpenAI to generate quiz questions from uploaded review modules.

## What it does
- Manage tasks, due dates, calendar, and upcoming deadlines
- Schedule local deadline reminders
- Dark mode
- Dashboard with a daily motivational message and a manually recorded study streak
- Upload a supported review module and generate multiple-choice practice questions through the online AI backend

## Important
This is a source project, not a prebuilt APK. The code has not been built or tested on your specific computer/device. Deploy the backend and configure its secret API key before testing AI quiz generation. AI API use may cost money. Do not upload private school or personal documents.

## Next steps
1. Read `backend/README.md` and deploy the backend.
2. Read `android/README.txt` and open the `android/` folder in Android Studio.
3. Build the debug APK in Android Studio.
4. In the app's AI Quiz screen, enter your deployed backend HTTPS URL.
