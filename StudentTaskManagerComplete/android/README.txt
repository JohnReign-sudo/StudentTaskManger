STUDENT TASK MANAGER — ANDROID STUDIO PROJECT
==============================================

Features included in this source project
- Add school tasks and optional subject names
- Choose due dates, calendar view, upcoming deadlines
- Mark tasks complete or delete them
- Dark mode
- Local device storage
- Android deadline reminder notifications
- AI Quiz Generator screen: choose a review file and send it to your deployed backend

IMPORTANT: This ZIP contains source code, not a compiled APK. The online quiz feature requires deploying the backend in ../backend and configuring an OpenAI API key there. Never embed the API key in the Android app.

BUILD THE ANDROID APP
1. Install Android Studio: https://developer.android.com/studio
2. Extract this ZIP and open the android folder (the one containing settings.gradle) in Android Studio.
3. Allow Gradle sync and install SDK Platform 35 if prompted. Use JDK 17 if prompted.
4. If Gradle sync succeeds, choose Build > Build Bundle(s) / APK(s) > Build APK(s).
5. The debug APK is usually app/build/outputs/apk/debug/app-debug.apk.
6. To run on a phone, connect it with USB debugging enabled and press Run.

SET UP AI QUIZ
1. Follow ../backend/README.md to deploy the backend.
2. Open the app and tap “AI Quiz Generator — upload a review module”.
3. Paste your backend HTTPS URL, choose a PDF/DOCX/PPTX/TXT review module, and tap Generate quiz.

The API key must be stored as a backend environment variable, never in the APK.
