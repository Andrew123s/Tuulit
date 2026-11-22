# TUULIT
## AI-Powered Menstrual Health, Wellness, Entertainment & Personal Care Application

---

# 1. PROJECT OVERVIEW

I want to build a complete Android application called **Tuulit**.

Tuulit is a modern menstrual cycle and wellness application inspired by the functionality and convenience of apps such as Flo, but with its own original identity, design, personality, features, and user experience.

Tuulit should not simply be a period-tracking application. It should combine menstrual tracking, personalized health insights, wellness, pain-management support, AI-powered health conversations, interactive games, quizzes, entertainment, emotional support, and optional connections with trusted people.

The goal is to create an application that users genuinely enjoy using, particularly during periods when they may be experiencing pain, discomfort, fatigue, mood changes, stress, boredom, or emotional difficulties.

Tuulit should help users:

- Track and understand their menstrual cycles.
- Predict upcoming periods based on recorded data.
- Record symptoms, moods, energy levels, and experiences.
- Understand changes in their bodies.
- Manage menstrual discomfort through guided wellness activities.
- Access reliable information about menstrual and reproductive health.
- Interact with an AI health and wellness assistant.
- Play games and complete entertaining quizzes.
- Discover personalized wellness recommendations.
- Connect with trusted partners, friends, or family members for optional support.
- Maintain a private journal.
- Discover personal patterns over time.
- Receive personalized experiences based on their preferences and cycle data.

The application should be intelligent, playful, supportive, private, beautiful, practical, and genuinely useful.

---

# 2. PRODUCT IDENTITY

## Application Name

Tuulit

## Brand Personality

Tuulit should feel:

- Warm.
- Friendly.
- Intelligent.
- Playful.
- Supportive.
- Modern.
- Calm.
- Trustworthy.
- Slightly humorous.
- Non-judgmental.
- Personal.

It should not feel overly clinical, childish, generic, or overly feminine in a stereotypical way.

The app should have a personality of its own.

---

# 3. LOGO AND MASCOT

## Logo Concept: Half-Peeled Banana

The Tuulit logo will be a half-peeled banana.

The banana represents:

- Comfort.
- Warmth.
- Humor.
- Softness.
- Personality.
- A little fun during difficult days.

Develop a distinctive logo around a half-peeled banana.

The banana may also become a mascot throughout the application.

Explore a minimalist banana character that can have different facial expressions, moods, poses, and appearances.

The mascot can be integrated into:

- Onboarding.
- Empty states.
- Games.
- Quizzes.
- Achievements.
- Notifications.
- Wellness activities.
- Loading animations.
- Mood-based interactions.
- Period-related experiences.

The mascot should be tasteful, recognizable, and visually consistent.

Do not make the app look like a children's game.

Create a brand identity that adults would enjoy using.

---

# 4. DESIGN SYSTEM AND COLORS

The entire application must follow the following color palette.

## Main Background — Cream / Beige

This is the primary background color.

Use it to create a warm, soft, natural, and premium interface.

## Primary UI and Text — Dark Charcoal

Use charcoal for:

- Primary text.
- Headings.
- Icons.
- Navigation.
- Important buttons.
- Dark UI components.

## Secondary Color — Warm Tan / Light Brown

Use it for:

- Cards.
- Secondary backgrounds.
- Illustrations.
- Supporting components.
- Borders.
- Visual grouping.
- Selected interface elements.

## Accent Color — Yellow

Use yellow selectively for:

- Banana illustrations.
- Highlights.
- Graphics.
- Gestures.
- Achievements.
- Interactive elements.
- Important visual moments.
- Playful animations.

Yellow should not overwhelm the interface.

## Design Requirements

- Cream/beige should remain the primary background.
- Charcoal should provide readability and contrast.
- Tan/light brown should support the design.
- Yellow should be used as an accent.
- Do not make pink the dominant color.
- Avoid generic purple AI interfaces.
- Avoid excessive gradients.
- Avoid multicolored interfaces.
- Avoid unnecessarily bright colors.
- Avoid excessive animations.
- Avoid crowded dashboards.
- Avoid childish illustrations.
- Avoid generic templates.

The application should feel like a premium lifestyle and wellness product.

Use:

- Modern typography.
- Excellent spacing.
- Rounded but sophisticated components.
- Beautiful cards.
- Simple illustrations.
- Subtle animations.
- Clear hierarchy.
- Minimalist layouts.
- Consistent iconography.

Every screen must follow the same visual language.

---

# 5. TECHNICAL REQUIREMENTS

Build the application primarily for Android.

Before implementation, evaluate and establish the appropriate technology stack.

Preferred technologies include:

- Kotlin.
- Jetpack Compose.
- Material 3 with a fully customized Tuulit design system.
- MVVM architecture.
- Room database.
- DataStore.
- Secure authentication.
- Modular feature architecture.
- Backend API.
- Secure cloud synchronization.
- Push notifications.
- AI integration.

The application should be:

- Modular.
- Maintainable.
- Scalable.
- Secure.
- Responsive.
- Accessible.
- Offline-capable where appropriate.
- Efficient.
- Easy to test.

Do not introduce technologies unnecessarily.

Do not automatically use Firebase without evaluating whether it is the most appropriate backend solution.

Keep the application architecture flexible enough to support future expansion.

---

# 6. APPLICATION NAVIGATION

Use a simple and intuitive navigation system.

Suggested bottom navigation:

1. Home
2. Calendar
3. Care
4. Play
5. Profile

AI should be accessible throughout the app.

Tuulit Connect should be accessible through the Home screen and Profile or a dedicated section, depending on the final UX architecture.

Navigation must be consistent.

Users should not have to search for basic functions.

---

# 7. ONBOARDING AND PERSONALIZATION

Create a welcoming onboarding experience.

The onboarding should introduce Tuulit without overwhelming the user.

## Onboarding Screens

### Screen 1 — Welcome to Tuulit

Introduce the application and its purpose.

### Screen 2 — Understand Your Cycle

Explain period tracking, cycle phases, symptoms, and personal insights.

### Screen 3 — Care and Comfort

Introduce pain-management support, wellness exercises, relaxation, and mood activities.

### Screen 4 — Play and Explore

Introduce games, quizzes, and entertaining experiences.

### Screen 5 — Your Personal Companion

Introduce Tuulit AI and its health and wellness support capabilities.

### Screen 6 — Stay Connected

Introduce the optional feature that allows users to connect trusted people for support.

### Screen 7 — Personalization

Allow users to set up their cycle information and preferences.

Collect relevant information such as:

- Last period start date.
- Average cycle length.
- Average period duration.
- Cycle regularity.
- Tracking preferences.
- Notification preferences.
- Wellness preferences.
- AI preferences.
- Optional support-sharing preferences.

Users must be able to skip information they do not know.

Do not assume users have regular cycles.

Do not make users feel uncomfortable if they cannot provide accurate information.

Explain that predictions are estimates.

---

# 8. HOME DASHBOARD

The Home screen should be the central experience of Tuulit.

Create a dynamic, personalized dashboard that adapts to the user's cycle, symptoms, preferences, and daily activity.

Display relevant information such as:

- Greeting.
- Current cycle day.
- Current cycle phase.
- Estimated next period.
- Period status.
- Today's mood.
- Energy level.
- Symptoms.
- Personalized suggestions.
- Quick logging.
- Care activities.
- Games.
- AI access.
- Relevant reminders.

Do not overcrowd the dashboard.

Prioritize important information.

Example:

"Hey, how are you feeling today?"

"Your period may arrive in approximately 4 days."

"Want to take a moment for yourself?"

The dashboard should change depending on whether the user is:

- Currently menstruating.
- Approaching their period.
- In the follicular phase.
- Near estimated ovulation.
- In the luteal phase.
- Experiencing symptoms.
- Having an irregular cycle.
- Logging their cycle for the first time.

Use contextual recommendations without assuming that all users experience cycle phases identically.

---

# 9. MENSTRUAL CYCLE TRACKER

Build a fully functional menstrual cycle tracking system.

## Period Tracking

Allow users to record:

- Period start date.
- Period end date.
- Flow intensity.
- Spotting.
- Period duration.

Flow categories:

- Light.
- Medium.
- Heavy.

Provide additional categories where appropriate for symptom tracking.

## Cycle Tracking

Record and calculate:

- Cycle length.
- Period duration.
- Cycle history.
- Cycle variability.
- Estimated next period.
- Cycle phase estimates.

Predictions must be based on available recorded information.

Do not hardcode cycle predictions.

Users should be able to correct cycle dates and update their records.

## Symptom Tracking

Allow users to record:

- Cramps.
- Headaches.
- Back pain.
- Breast tenderness.
- Bloating.
- Acne.
- Fatigue.
- Nausea.
- Diarrhea.
- Constipation.
- Dizziness.
- Food cravings.
- Appetite changes.
- Sleep problems.
- Mood changes.
- Other custom symptoms.

Allow users to select symptom severity.

## Mood Tracking

Include moods such as:

- Happy.
- Sad.
- Irritated.
- Anxious.
- Calm.
- Emotional.
- Stressed.
- Energetic.
- Low.
- Neutral.

Allow users to create custom moods.

## Energy Tracking

Allow users to record:

- Very low energy.
- Low energy.
- Normal energy.
- High energy.
- Very high energy.

## Lifestyle Tracking

Optional entries may include:

- Sleep quality.
- Exercise.
- Water intake.
- Libido.
- Sexual activity.
- Stress.
- Medication use.
- Other lifestyle observations.

All sensitive logging must be optional.

## Journal

Create a private daily journal where users can record:

- Thoughts.
- Experiences.
- Emotions.
- Symptoms.
- Personal notes.
- Things they want to remember.

Allow users to edit and delete journal entries.

---

# 10. CALENDAR

Develop a fully interactive menstrual calendar.

The calendar should display:

- Period days.
- Predicted period days.
- Estimated fertile window.
- Estimated ovulation.
- Logged symptoms.
- Mood entries.
- Energy levels.
- Notes.

Users should be able to:

- Navigate months.
- View previous cycles.
- View upcoming predictions.
- Select a date.
- Add entries.
- Edit entries.
- Delete entries.
- View a detailed day summary.

Clearly distinguish between recorded information and predicted information.

Predictions must never be presented as certainty.

The application must not imply that estimated fertile windows or ovulation dates can be used as reliable contraception.

---

# 11. TUULIT CARE

Create a dedicated wellness section called Tuulit Care.

This section should help users feel better physically, mentally, and emotionally.

## 11.1 Cramp Relief

Develop an interactive cramp-support experience.

Users should be able to select their discomfort level.

### Mild Discomfort

Provide:

- Gentle breathing.
- Short relaxation.
- Heat suggestions.
- Comfortable movement.

### Moderate Discomfort

Provide:

- Guided relaxation.
- Gentle stretching.
- Heat therapy information.
- Body-position suggestions.
- Appropriate pain-management information.

### Severe Discomfort

Provide:

- Supportive guidance.
- Safe symptom information.
- Appropriate medical guidance.
- Warning signs that may require urgent attention.

Do not dismiss severe pain.

Do not present wellness activities as guaranteed treatments.

## 11.2 Relaxation Studio

Develop:

- Guided breathing.
- Meditation.
- Body relaxation.
- Stress relief.
- Anxiety support.
- Mindfulness.
- Sleep relaxation.

Sessions should be available in different lengths:

- 1 minute.
- 3 minutes.
- 5 minutes.
- 10 minutes.

## 11.3 Sleep Support

Provide:

- Relaxing sounds.
- Sleep meditation.
- Breathing exercises.
- Sleep routines.
- Personal sleep notes.
- Relaxation activities.

## 11.4 Mood Reset

Create an interactive Mood Reset experience.

The user selects their current emotional state.

Tuulit recommends a suitable activity.

Examples:

- Feeling overwhelmed → breathing exercise.
- Feeling irritated → calming activity.
- Feeling sad → supportive message and journal.
- Feeling exhausted → low-energy activity.
- Feeling bored → quick game.
- Feeling anxious → grounding exercise.

Avoid toxic positivity.

Do not automatically attribute emotional experiences to hormonal changes.

## 11.5 Personalized Comfort Recommendations

Based on user preferences, symptoms, and recorded information, suggest appropriate wellness activities.

Recommendations must be realistic and not medically misleading.

---

# 12. TUULIT PLAY

Develop a dedicated games and entertainment section.

The purpose is to provide enjoyable distractions, especially when users are bored, uncomfortable, tired, or simply looking for entertainment.

Games must be functional and genuinely playable.

## 12.1 Games

### Banana Catch

Catch falling bananas while avoiding other objects.

### Mood Match

Match cards containing emotions, illustrations, and wellness-themed objects.

### Brain Break

Quick memory and concentration games.

### Word Puzzle

Find words related to wellness, nature, food, emotions, and everyday life.

### Calm Tap

A relaxing interactive rhythm or tapping experience.

### Memory Flip

A card-matching game.

### Stress Smash

A playful activity involving popping, crushing, or swiping abstract shapes.

### Focus Game

Short interactive games designed to provide distraction and entertainment.

Develop more games as the platform evolves.

## 12.2 Quizzes

Create engaging quizzes such as:

- How well do you know your menstrual cycle?
- Period myths versus facts.
- What is your period personality?
- What kind of self-care do you need today?
- What is your mood telling you?
- Menstrual health knowledge.
- Body awareness.
- Wellness habits.

Quizzes should be informative, funny, and engaging without being judgmental.

## 12.3 Rewards

Introduce a light reward system.

Users may earn:

- Points.
- Badges.
- Achievements.
- Unlockable illustrations.
- Banana expressions.
- Optional daily challenges.

Avoid excessive gamification.

Do not make users feel guilty for missing days.

---

# 13. TUULIT AI — INTELLIGENT HEALTH ASSISTANT

Create a real AI assistant integrated into Tuulit.

It must not be a fake chatbot or a collection of hardcoded responses.

The AI should support conversations about:

- Menstrual health.
- Period symptoms.
- Cramps.
- PMS.
- PMDD information.
- Cycle irregularities.
- Ovulation.
- Hormonal changes.
- Reproductive health.
- Vaginal health.
- Sexual health education.
- Emotional wellbeing.
- Sleep.
- Lifestyle.
- Wellness.
- General health questions.

## AI PERSONALITY

The assistant should be:

- Warm.
- Intelligent.
- Calm.
- Natural.
- Friendly.
- Non-judgmental.
- Clear.
- Supportive.

Avoid overly robotic responses.

Avoid excessively long explanations unless requested.

## PERSONALIZATION

With explicit permission, the AI may use relevant user information such as:

- Cycle phase.
- Period history.
- Logged symptoms.
- Mood.
- Energy.
- Preferences.
- Relevant previous conversations.

For example, when a user asks why they are experiencing fatigue, the AI may consider their logged information while also explaining that fatigue can have multiple causes.

Do not assume every symptom is caused by the menstrual cycle.

## AI FEATURES

### Ask Tuulit

Conversational health and wellness assistant.

### Symptom Explanation

Explain common symptoms, possible causes, and when to seek medical attention.

### Cycle Education

Explain different cycle phases and potential changes.

### Personal Insights

Help users understand recurring patterns based on sufficient recorded data.

### Wellness Recommendations

Suggest appropriate activities based on user preferences and symptoms.

### Health Myth Checker

Allow users to ask whether menstrual and reproductive health information is accurate.

### Appointment Preparation

Help users prepare questions for medical appointments.

### Health Summary

With the user's permission, generate a structured summary of recorded symptoms and experiences.

## MEDICAL SAFETY

The AI must:

- Never claim to diagnose a condition.
- Avoid inventing scientific information.
- Prioritize trustworthy medical information.
- Clearly communicate uncertainty.
- Recognize concerning symptoms.
- Identify urgent warning signs.
- Never dismiss severe pain.
- Avoid unsafe treatment recommendations.
- Avoid assumptions about gender, sexual activity, pregnancy, or reproductive intentions.
- Never shame users.
- Provide appropriate medical guidance when necessary.

Implement a secure AI backend.

Protect API keys.

Do not expose AI credentials in the Android application.

Make the AI provider replaceable.

---

# 14. TUULIT CONNECT

Develop an optional feature called Tuulit Connect.

This feature allows users to connect with people they trust.

Possible connections include:

- Partners.
- Boyfriends.
- Girlfriends.
- Husbands.
- Wives.
- Friends.
- Sisters.
- Family members.

The feature must be entirely consent-based.

## 14.1 Invitations

Users should be able to:

1. Select someone they trust.
2. Generate an invitation.
3. Share it through an appropriate channel.
4. Allow the recipient to accept.
5. Establish a connection.
6. Configure sharing permissions.

## 14.2 Sharing Controls

The primary user must control what the connected person can see.

Possible information:

- Period approaching.
- Period currently ongoing.
- Estimated period end.
- General cycle phase.
- Selected moods.
- Selected symptoms.
- Support requests.

Sensitive information must not be shared automatically.

Users must be able to revoke access immediately.

## 14.3 Partner Experience

Create a simplified interface for connected people.

The interface may display messages such as:

"Your person may appreciate a little extra support today."

Provide suggestions such as:

- Ask how they are feeling.
- Offer a warm drink.
- Prepare a heating pad.
- Offer food.
- Help with chores.
- Plan a relaxing evening.
- Give them space.
- Be patient.
- Ask rather than assume.

Do not encourage monitoring or controlling behavior.

## 14.4 Support Requests

Allow users to send requests such as:

- I need comfort.
- I need food.
- I need help.
- I need space.
- I need a distraction.
- Check on me later.
- I am having a difficult day.
- Can we talk?

Make these requests simple and optional.

---

# 15. INNOVATIVE FEATURE — "I NEED SOMETHING"

Create a feature called:

## I Need Something

This should be an accessible feature on the Home screen.

When the user taps it, Tuulit asks:

"What do you need right now?"

Options:

- I'm in pain.
- I'm irritated.
- I'm sad.
- I'm exhausted.
- I'm bored.
- I can't sleep.
- I need someone.
- I don't know.

Tuulit then provides a suitable experience.

Examples:

| User selection | Experience |
|---|---|
| I'm in pain | Comfort guidance, heat suggestions, relaxation |
| I'm irritated | Calming exercise |
| I'm sad | Supportive message and journal |
| I'm exhausted | Low-energy activities |
| I'm bored | Quick game |
| I can't sleep | Sleep relaxation |
| I need someone | AI conversation or trusted contact |
| I don't know | Gentle mood exploration |

The feature should feel personal and useful.

Do not give the same response to every selection.

---

# 16. INNOVATIVE FEATURE — TUULIT SOS

Develop a feature that allows users to quickly communicate their needs to their trusted person.

Possible messages:

- Today is a rough one.
- I need a hug.
- I need food.
- Check on me later.
- I need space.
- Help me with something.
- Let's watch something.
- Call me.
- I'm not feeling well.

The user should be able to send these messages without having to type.

This feature must integrate with Tuulit Connect.

Sharing should only occur with the user's explicit permission.

---

# 17. INNOVATIVE FEATURE — CYCLE MOOD MAP

Develop a personalized Cycle Mood Map.

This feature should visualize how the user experiences different stages of their cycle.

Over time, it may reveal patterns involving:

- Mood.
- Energy.
- Symptoms.
- Sleep.
- Cravings.
- Pain.
- Stress.
- Activity.

Example:

The user may notice that fatigue is frequently logged near the end of their cycle or that headaches occur around their period.

Tuulit may provide insights such as:

"You have recorded lower energy during the final few days of your cycle in four of your last five cycles."

Or:

"Headaches have appeared more frequently around the beginning of your period."

These insights must be based on actual recorded data.

Do not fabricate patterns.

Do not generalize from insufficient data.

The visualization should be simple, clear, attractive, and interactive.

---

# 18. INNOVATIVE FEATURE — BANANA MODE

Create an optional playful experience called Banana Mode.

The banana mascot can have different expressions and personalities based on the selected experience.

Examples:

### Chill Banana
Relaxation, meditation, and sleep.

### Drama Banana
Funny distractions, entertainment, and playful content.

### Sleepy Banana
Low-energy activities and rest.

### Gym Banana
Gentle movement and stretching.

### Cramps Banana
Comfort activities and pain-management guidance.

### Happy Banana
Celebrations, achievements, and positive experiences.

Banana Mode should be playful and visually interesting.

Avoid making the entire application cartoonish.

---

# 19. INNOVATIVE FEATURE — PERIOD SURVIVAL KIT

Create a personalized Period Survival Kit.

Users should be able to save their favorite comfort activities and resources.

The kit may contain:

- Favorite relaxation exercises.
- Favorite games.
- Comfort playlists.
- Heating pad reminders.
- Food suggestions.
- Sleep routines.
- Personal notes.
- AI conversations they want to revisit.
- Trusted contact.
- Comfort activities.
- Personal wellness preferences.

Allow users to customize their survival kit.

Example:

"My Period Survival Kit"

- Heating pad.
- Favorite playlist.
- Banana Catch.
- Chocolate.
- 5-minute meditation.
- Comfort movie.
- Journal.
- Contact my partner.

The kit should be personalized rather than generic.

---

# 20. INNOVATIVE FEATURE — EXPLAIN MY BODY

Create an AI-powered educational experience called Explain My Body.

Users can ask questions and receive clear explanations about their bodies.

Example:

User:

"Why do I feel bloated before my period?"

The AI provides an understandable explanation.

Then display optional follow-up buttons:

- Explain it simply.
- What can I do?
- Is this common?
- When should I worry?
- Tell me more.

This should make health education accessible and easy to understand.

Avoid excessive medical terminology.

Do not oversimplify important medical information.

---

# 21. INNOVATIVE FEATURE — PAIN JOURNAL AND PATTERN DETECTION

Create a dedicated Pain Journal.

Allow users to record:

- Pain intensity.
- Pain location.
- Pain type.
- Duration.
- Associated symptoms.
- Medication taken, if applicable.
- Impact on daily activities.
- Whether they were able to work, study, exercise, or sleep.

Over time, Tuulit may identify recurring patterns.

Example:

"Your pain has been rated 8/10 during three consecutive cycles and has interfered with your daily activities."

Provide an option to generate a structured health summary.

The summary may contain:

- Cycle dates.
- Pain frequency.
- Pain intensity.
- Symptoms.
- Duration.
- Impact on daily activities.
- Relevant observations.

Allow the user to export or share the summary if they choose.

This feature should help users organize their health information and prepare for medical appointments.

Do not use pattern detection to diagnose medical conditions.

---

# 22. INNOVATIVE FEATURE — COUPLE MODE

Create an optional Couple Mode as part of Tuulit Connect.

The purpose is to help a partner understand menstrual health and become more supportive.

Provide educational content about:

- Menstrual cramps.
- PMS.
- Cycle variability.
- Symptoms.
- Fatigue.
- Emotional wellbeing.
- Communication.
- Supportive behavior.
- Common misconceptions.

Include a playful quiz:

"How good are you at period support?"

The quiz should teach partners useful information.

Examples:

- What should you do if your partner is having cramps?
- Is everyone's menstrual cycle identical?
- Should you assume someone wants advice or ask them?
- What does supportive communication look like?

Couple Mode should be optional and supportive.

Do not make the partner the authority over the user's cycle.

---

# 23. INNOVATIVE FEATURE — STREAKS WITHOUT PRESSURE

Create a light achievement system.

Achievements may include:

- First cycle tracked.
- First mood check-in.
- First journal entry.
- Completed a relaxation exercise.
- Played a game.
- Completed a quiz.
- Learned something new.
- Created a Period Survival Kit.
- Explored cycle insights.

Do not penalize users for missing days.

Avoid streak-loss notifications.

The app should support users rather than pressure them.

---

# 24. INNOVATIVE FEATURE — "TODAY IS NOT THE DAY" MODE

Create a special minimal experience.

When users feel exhausted, overwhelmed, uncomfortable, or simply do not want to interact with the app, they can tap:

## Today Is Not the Day

Tuulit should switch to a minimal interface.

Display:

"Let's keep things simple today."

Offer only a few actions:

- Log how I'm feeling.
- Find something to distract me.
- Relax for five minutes.
- Ask Tuulit something.
- Tell my person.
- Rest.

Reduce visual complexity.

Avoid unnecessary notifications.

Make the experience feel calm.

---

# 25. ADDITIONAL FEATURES

Implement or plan the following features.

## Voice Journaling

Allow users to record how they feel using voice.

Where technically appropriate, provide transcription so they can save the recording as a journal entry.

Voice data must be handled securely.

## Cycle-Based Food Suggestions

Provide personalized food and meal suggestions based on user preferences and symptoms.

Avoid claiming that particular foods cure hormonal problems or menstrual disorders.

## Period Emergency Checklist

Create a useful checklist for unexpected periods.

Possible items:

- Menstrual products.
- Spare underwear.
- Wipes.
- Pain-management supplies.
- Plastic or reusable storage.
- Personal essentials.

Allow users to customize their checklist.

## Private Mode

Allow users to protect sensitive information using appropriate app-lock functionality.

Explore:

- PIN.
- Biometric authentication.
- Privacy controls.
- Hidden notification content.

## Personal Comfort Library

Allow users to save:

- Activities.
- Music.
- Exercises.
- Games.
- Journal prompts.
- Wellness tips.
- Comfort routines.

## Symptom Export

Allow users to export cycle and symptom history in an organized format.

Possible formats:

- PDF.
- CSV.

## AI Appointment Preparation

Help users prepare for medical appointments by organizing:

- Symptoms.
- Cycle dates.
- Pain history.
- Questions.
- Concerns.

## Cycle-Aware Activity Suggestions

Suggest activities based on recorded symptoms, energy, preferences, and cycle information.

Do not rigidly assume what users should be doing during a particular cycle phase.

## Android Home-Screen Widget

Create an optional widget that may display:

- Next period estimate.
- Cycle day.
- Daily wellness message.
- Quick logging.

Provide privacy controls so users can choose what appears on the home screen.

## Wearable Integration

Consider future integration with supported health platforms and wearables for data such as:

- Sleep.
- Activity.
- Heart rate, where appropriate.
- Other user-authorized health data.

Do not make wearable integration mandatory.

---

# 26. TUULIT INSIGHTS

Develop an Insights section that helps users understand their menstrual and wellness patterns.

Display:

- Average cycle length.
- Cycle variability.
- Average period duration.
- Cycle history.
- Symptom frequency.
- Mood patterns.
- Energy patterns.
- Pain patterns.
- Sleep patterns.
- Personal trends.

Use charts and visualizations.

Keep the design simple.

Do not overwhelm users with statistics.

All insights must be based on actual recorded information.

Do not fabricate data.

Do not present correlations as medical causation.

Clearly indicate when the app does not have sufficient information to generate an insight.

---

# 27. PERSONALIZATION

Allow users to customize:

- Symptoms.
- Moods.
- Notifications.
- Cycle preferences.
- Wellness preferences.
- Theme settings.
- AI preferences.
- Privacy.
- Sharing permissions.
- Comfort library.
- Period survival kit.

Personalization should improve the experience over time.

It should not become intrusive.

---

# 28. NOTIFICATIONS

Develop a customizable notification system.

Possible notifications:

- Your period may be approaching.
- Your period may start today.
- Your period has been logged.
- How are you feeling today?
- Remember to log your symptoms.
- Take a moment for yourself.
- Your daily quiz is ready.
- A new game is available.
- You may want to rest.
- Your trusted person has received a support request.

Users must be able to:

- Enable or disable notifications.
- Configure notification frequency.
- Customize notification categories.
- Set quiet hours.
- Hide sensitive information from lock screens.

Avoid excessive notifications.

---

# 29. PRIVACY AND SECURITY

Tuulit will handle highly sensitive menstrual, reproductive, emotional, and potentially sexual-health-related information.

Privacy must be a core architectural requirement.

Implement:

- Secure authentication.
- Encryption in transit.
- Secure data storage.
- Protected local storage where appropriate.
- Access control.
- Permission-based sharing.
- Minimal data collection.
- Secure AI communication.
- Account deletion.
- Data deletion.
- Export functionality.
- Privacy controls.
- Secure invitation handling.
- Protection against unauthorized access.

Users must understand:

- What information is stored.
- What information is processed by AI.
- What information is shared.
- Who can access their data.
- How to revoke sharing.
- How to delete their data.

Never share sensitive information without explicit consent.

Never expose private cycle data through public profiles.

---

# 30. ACCESSIBILITY

The application must support:

- Readable typography.
- High contrast.
- Screen readers.
- Adjustable text size.
- Accessible buttons.
- Clear navigation.
- Reduced motion.
- Accessible charts.
- Simple language.
- Touch-friendly interaction.

Do not rely exclusively on color to communicate information.

---

# 31. REQUIRED SCREENS

Develop the following screens as part of the application.

## Authentication

- Welcome.
- Registration.
- Login.
- Password recovery.

## Onboarding

- Introductory screens.
- Cycle setup.
- Personalization.
- Notification preferences.
- AI introduction.
- Optional Connect introduction.

## Home

- Dashboard.
- Cycle overview.
- Today's mood.
- Energy.
- Symptoms.
- Recommendations.
- Quick logging.
- I Need Something.
- Today Is Not the Day.

## Calendar

- Monthly calendar.
- Cycle visualization.
- Predicted periods.
- Daily records.
- Day details.

## Tracking

- Period logging.
- Symptom logging.
- Mood logging.
- Energy logging.
- Lifestyle logging.
- Journal.
- Pain journal.

## Care

- Cramp relief.
- Relaxation.
- Breathing.
- Stretching.
- Sleep support.
- Mood reset.
- Comfort activities.
- Food suggestions.

## Play

- Games.
- Quizzes.
- Challenges.
- Rewards.
- Achievements.

## AI

- AI chat.
- Health questions.
- Explain My Body.
- Health myth checker.
- Cycle education.
- Personalized insights.
- Appointment preparation.

## Connect

- Invitations.
- Connected users.
- Sharing permissions.
- Partner experience.
- Support requests.
- Couple Mode.

## Insights

- Cycle history.
- Mood patterns.
- Symptom patterns.
- Energy patterns.
- Pain trends.
- Charts.

## Profile

- Personal information.
- Settings.
- Notifications.
- Privacy.
- AI preferences.
- Connected users.
- Comfort library.
- Period Survival Kit.
- Export data.
- Account management.

---

# 32. DEVELOPMENT PHASES

Develop Tuulit through controlled phases.

Do not build everything at once.

## PHASE 1 — PRODUCT BLUEPRINT

Before coding, produce:

- Product requirements.
- Target users.
- Product vision.
- Feature architecture.
- MVP definition.
- Development roadmap.
- Technical stack.
- Android architecture.
- Backend architecture.
- AI architecture.
- Database design.
- Privacy architecture.
- User journeys.
- Navigation structure.
- Screen inventory.
- Technical risks.

Wait for approval.

## PHASE 2 — BRAND AND DESIGN SYSTEM

Develop:

- Logo.
- Banana mascot.
- Color system.
- Typography.
- UI components.
- Navigation.
- Illustrations.
- Animations.
- Design guidelines.

Create a cohesive design system before building all screens.

## PHASE 3 — ANDROID FOUNDATION

Implement:

- Android project.
- Kotlin.
- Jetpack Compose.
- Navigation.
- Theme.
- Architecture.
- Database.
- Authentication foundation.
- Reusable components.

Ensure the application builds and runs.

## PHASE 4 — CORE TRACKING

Implement:

- User profile.
- Cycle setup.
- Period logging.
- Calendar.
- Cycle calculations.
- Symptom tracking.
- Mood tracking.
- Energy tracking.
- Journal.
- Pain journal.

Test data persistence.

## PHASE 5 — TUULIT CARE

Implement:

- Cramp relief.
- Relaxation.
- Breathing.
- Sleep support.
- Mood reset.
- Comfort library.
- Personalized recommendations.

## PHASE 6 — TUULIT PLAY

Implement:

- Games.
- Quizzes.
- Scoring.
- Achievements.
- Challenges.
- Rewards.

Ensure every game is functional.

## PHASE 7 — TUULIT AI

Implement:

- Backend AI integration.
- Chat interface.
- Secure AI communication.
- Health knowledge architecture.
- Personalization.
- Conversation history.
- AI safety.
- Explain My Body.
- Appointment preparation.

## PHASE 8 — TUULIT CONNECT

Implement:

- Invitation system.
- User connections.
- Permissions.
- Shared information.
- Support requests.
- Partner experience.
- Couple Mode.

Test privacy permissions thoroughly.

## PHASE 9 — ADVANCED FEATURES

Implement:

- Cycle Mood Map.
- Banana Mode.
- Period Survival Kit.
- Today Is Not the Day.
- I Need Something.
- Tuulit SOS.
- Insights.
- Voice journaling.
- Symptom export.
- Home-screen widget.

Prioritize these according to feasibility and user value.

## PHASE 10 — TESTING

Perform:

- Unit tests.
- UI tests.
- Database tests.
- Cycle calculation tests.
- AI integration tests.
- AI safety tests.
- Notification tests.
- Security tests.
- Privacy tests.
- Invitation tests.
- Sharing permission tests.
- Offline tests.
- Performance tests.

## PHASE 11 — FINAL POLISH

Improve:

- Design consistency.
- UX.
- Accessibility.
- Animations.
- Loading states.
- Empty states.
- Error handling.
- Performance.
- Navigation.
- Reliability.

Prepare Tuulit for release.

---

# 33. FEATURE PRIORITIZATION

Categorize features into four development stages.

## MVP — FIRST RELEASE

Prioritize:

- Onboarding.
- User profile.
- Period tracker.
- Cycle calendar.
- Cycle calculations.
- Symptom logging.
- Mood logging.
- Energy logging.
- Home dashboard.
- Basic Care.
- A few functional games.
- Quizzes.
- Basic AI health assistant.
- Notification settings.
- Privacy settings.
- Local data persistence.

The MVP must be a complete, usable application.

## V1 — EXPANSION

Add:

- Advanced Care.
- Additional games.
- AI personalization.
- Period Survival Kit.
- Personal Comfort Library.
- Cycle Mood Map.
- Insights.
- Pain Journal.
- Symptom export.
- I Need Something.
- Today Is Not the Day.

## V2 — CONNECTED EXPERIENCES

Add:

- Tuulit Connect.
- Invitations.
- Support requests.
- Partner interface.
- Couple Mode.
- Tuulit SOS.
- Advanced AI appointment preparation.
- Voice journaling.
- Cloud synchronization.

## FUTURE POSSIBILITIES

Explore:

- Wearables.
- Health platform integrations.
- Advanced personalization.
- Additional AI features.
- More games.
- Expanded educational experiences.
- Optional community experiences, if privacy and moderation can be handled responsibly.

Do not attempt to implement every feature in the MVP.

---

# 34. DEVELOPMENT RULES

Follow these rules throughout the project.

1. Maintain a clear architecture.
2. Do not rush development.
3. Do not build the entire application in one uncontrolled coding session.
4. Do not skip testing.
5. Do not generate unnecessary files.
6. Do not duplicate functionality.
7. Do not create fake functionality.
8. Do not leave broken navigation.
9. Do not leave dead buttons.
10. Do not use hardcoded fake data in finished features.
11. Do not hardcode cycle predictions.
12. Do not claim AI functionality exists when it is mocked.
13. Do not claim a feature works unless it has been tested.
14. Do not expose API keys.
15. Do not compromise privacy.
16. Do not introduce unnecessary infrastructure.
17. Maintain the Tuulit design system.
18. Ensure features are modular.
19. Test each phase before moving to the next.
20. Maintain a development log.
21. Maintain a checklist of completed, pending, blocked, and untested tasks.
22. Identify technical limitations early.
23. Make reasonable decisions instead of constantly asking unnecessary questions.
24. Ask for approval at major milestones.
25. If a feature is too complex for the current phase, document it and propose a realistic alternative rather than implementing a poor version.

---

# 35. FIRST TASK — DO NOT START CODING YET

Your first responsibility is to develop a comprehensive **Tuulit Product Blueprint**.

Produce the following:

1. Product vision.
2. Target audience.
3. User problems.
4. Unique value proposition.
5. Competitive positioning.
6. Complete feature architecture.
7. Feature prioritization.
8. MVP definition.
9. V1 roadmap.
10. V2 roadmap.
11. Future feature roadmap.
12. Android technology stack.
13. Android architecture.
14. Backend architecture.
15. AI architecture.
16. Database structure.
17. Privacy architecture.
18. Security architecture.
19. Navigation architecture.
20. Complete screen inventory.
21. Design system.
22. Brand identity.
23. Banana mascot concept.
24. User journeys.
25. Technical risks.
26. Testing strategy.
27. Development roadmap.
28. Recommended order of implementation.

Be detailed but practical.

Clearly identify features that require:

- Backend development.
- AI integration.
- Cloud infrastructure.
- External APIs.
- Notifications.
- Authentication.
- Data synchronization.
- Third-party services.

Do not begin implementation until the Product Blueprint is complete and reviewed.

After producing the blueprint, stop and wait for approval.

---

# FINAL PRODUCT GOAL

Tuulit should become more than a period tracker.

It should be a trusted personal companion that helps users understand their bodies, manage difficult days, find comfort, learn, relax, play, and connect with people they trust.

The app should combine practical functionality with a distinctive personality.

It should be intelligent without being intrusive, playful without being childish, supportive without being overwhelming, and medically informative without pretending to diagnose.

Build Tuulit as a real application—not merely a collection of beautiful mockup screens.

Every feature must have a clear purpose, and every implemented feature must work.