# Tuulit Privacy Policy

> Generated from the published page https://tuulit.app/privacy (source: `website/src/pages/privacy.astro`). Edit the page, rebuild, then regenerate this file.

**Version 1.0**
**Effective 24 November 2026**
**Last updated 24 November 2026**

## Summary

- **Tuulit works without an account.** Then your cycle, mood, symptom and journal records stay in an encrypted database on your phone, and we never receive them.
- **Optional features send data only when you use them:** account backup, Ask Tuulit, Tuulit Connect and voice-note backup.
- **We don't sell your data, show ads, or use analytics or advertising trackers in the app.**
- **You can export your data, delete it from your phone, and delete your account and cloud backup** from inside the app at any time.

This policy applies to the Tuulit app for Android (and the iPhone version when it is released) and to this website, tuulit.app.

## Who is responsible

Tuulit is operated by **Ycomspace**, Germany ("we", "us"). We are the controller of the personal data described in this policy. You can reach us at [ycomspacegh@gmail.com](mailto:ycomspacegh@gmail.com).

## Data that stays on your phone

Everything you log is stored on your device first, in an encrypted database. On Android the database is encrypted with a key held in the Android Keystore; on iPhone it is protected by iOS Data Protection. The app excludes this data from the phone's automatic cloud backups.

On your phone, Tuulit stores:

- **Cycle records:** period start and end dates, and predictions calculated on the device.
- **Daily logs:** flow, mood score and mood words, symptoms and their severity, energy, sleep quality, water, medication and movement notes, and your private journal notes.
- **Pain journal entries:** intensity, location, type, duration, relief tried, the activities affected, and notes.
- **Voice notes** you choose to record (audio files kept in the app's private storage).
- **Your Comfort Library, survival kit, game scores and earned "moments".**
- **Your Ask Tuulit conversation history.** It is kept only on your phone and is never backed up to the cloud.
- **Settings,** such as your name, cycle defaults, reminder preferences and your app-lock PIN. The PIN is stored only as a salted hash, never as the PIN itself.

Data that only ever exists on your phone is not sent to us. Uninstalling the app, or using *You → Privacy → Delete all data on this phone*, erases it.

## Data we process when you use optional features

  | Feature | Data | Why |
|---|---|---|
  | Account (optional) | Email address, a hashed password (held by Firebase Authentication), optional display name, account identifiers, sign-in timestamps | To create and secure your account and let you sign in on another phone, including across Android and iPhone |
 | Cloud backup (automatic once you have an account) | Your cycle records, daily logs (including journal notes), pain journal entries, Comfort Library and kit items, game scores, moments, a reference to backed-up voice notes, and the settings that follow you between devices (name, cycle defaults, regularity, Banana Mode, custom moods and symptoms, SOS messages, sleep routine, and whether Ask may use your summary) | To restore your data on a new phone and keep your phones in sync |
 | Emails | Email address and display name | To send the account emails you need: a welcome email, email verification, password reset and confirmation of account deletion. We don't send marketing emails. |
 | Ask Tuulit | Your question, up to the last 12 messages of that conversation, your language setting and, only if you switch on "Personal answers", a short summary (see [Ask Tuulit](#ai)) | To answer your question |
 | Tuulit Connect | Names, invite codes, the connection and its permissions, and the status fields you allow (see [Tuulit Connect](#connect)) | To share the updates you choose with the people you choose |
 | Notifications | A push token for your device, the platform (Android or iOS) and when it was last updated | To deliver Connect messages such as an SOS or a reply. Period and check-in reminders are scheduled on your phone and need no server. |
 | Voice-note backup (optional, per recording) | The audio file you choose to back up | To keep a private copy you can restore |
 | App integrity | An attestation token from Google Play Integrity (Android) or Apple App Attest (iPhone) | To check that requests to our server come from the genuine app, which protects against abuse |
 | Server logs and limits | Request metadata processed by our hosting provider; event types such as "question answered" or "account deleted", without the content; a daily usage counter linked to your account identifier | To run the service securely, limit abuse and fix problems |
 | Contact form on this website | Your name, email address, topic and message. Your IP address is kept for about 26 hours in a counter that limits how many messages can be sent in a day. | To reply to you, and to prevent spam |
 | Visiting this website | Technical data your browser sends (such as IP address, browser type and the page requested), processed by our host, Cloudflare. The website sets no cookies and uses no analytics. | To deliver the website securely |

We don't use the app to collect your location, contacts, photos, advertising identifiers or analytics. The app requests only these Android permissions: internet, notifications (for reminders and Connect), and microphone (only when you choose to record or dictate).

**Dictation.** When you dictate a note, the speech is converted to text by your phone's speech recognition service. Tuulit asks for on-device recognition where the phone supports it; otherwise your phone's provider (Google on Android, Apple on iPhone) may process the audio under its own terms. The text goes into your note on the phone.

## Health data and our legal bases

Menstrual, reproductive, mood and symptom information is health data, a special category of personal data under the EU General Data Protection Regulation (GDPR). We process it in the cloud only when you choose a feature that needs it, and on the basis of your **explicit consent** (Art. 9(2)(a) GDPR). You can withdraw consent at any time by deleting your account and backup, switching off "Personal answers in Ask", or disconnecting a trusted person. Withdrawing doesn't affect processing that happened before.

Our other legal bases are:

- **Performance of our agreement with you** (Art. 6(1)(b) GDPR) for providing your account, backup, Connect and the features you request.
- **Our legitimate interests** (Art. 6(1)(f) GDPR) in keeping the service secure, preventing abuse (app integrity checks and usage limits) and answering enquiries.
- **Legal obligations** (Art. 6(1)(c) GDPR) where the law requires us to keep or disclose information.

We never use your health data for advertising, and we never sell it.

## Ask Tuulit (AI)

Ask Tuulit needs an account. When you ask a question:

- The app sends your question, up to the previous 12 messages of that conversation and your language setting to our server (a Cloudflare Worker). The server passes them to our AI provider, currently **Google's Gemini API**, to generate an answer.
- If you switch on *Personal answers in Ask* (off by default), the request also includes a short summary of your own logs: your current cycle day and phase, average cycle and period length, how much your cycles vary, your most-logged symptoms, and how many cycles had severe pain. It never includes your journal notes or the dates of individual entries.
- **Appointment prep** sends only the summary shown on screen, plus the concern you type, and only when you tap "Suggest questions".
- Before sending, your phone checks your question for urgent warning signs and shows safety guidance. The server repeats this check.
- Your conversation history is stored only on your phone. Our server doesn't keep the content of questions or answers; it logs only that an answer was given, the tool used, the urgency level and whether a summary was included.

Answers are general information, not medical advice or a diagnosis.

## Tuulit Connect

Connect lets you share a small, controlled status with people you trust. Both people need an account.

- When you invite someone, we store a one-time code with your account identifier and display name. The code expires after 24 hours.
- When they accept, we store the connection: both account identifiers and display names, the relationship they chose, and your sharing permissions. **Every permission starts switched off.**
- You decide what they can see, and only these things can ever be shared: that your period is coming soon (a heads-up two days before), that it has started (with the date), your current cycle phase, and a rough-day signal with a short message of up to 140 characters when you send an SOS.
- **Your symptoms, moods, journal notes and pain entries are never shared through Connect.**
- Switching a permission off removes it from their view. Either of you can disconnect at any time, which deletes the connection and the shared status.
- Your trusted person can send you a short reply. A notification is sent to their phone or yours through our server and Firebase Cloud Messaging, containing the sender's name and the message.

## Service providers

We use these providers to run Tuulit. They process data on our behalf and only for the purposes above.

  | Provider | Service | Data |
|---|---|---|
  | Google (Firebase) | Authentication, Cloud Firestore database (located in a European multi-region), Cloud Messaging, App Check | Account, backup, Connect, push tokens, integrity tokens |
 | Google (Gemini API) | AI answers for Ask Tuulit | Questions, recent messages and the optional summary |
 | Cloudflare | Our server (Workers), usage counters (KV), voice-note storage (R2), hosting of this website | Requests passing through our server, usage counters, backed-up voice notes, contact-form messages in transit |
 | Brevo | Sending account emails and forwarding website enquiries to us | Email address, display name, email content |

Some of these providers are based in, or may process data in, countries outside the European Economic Area, including the United States. Where that happens, the transfer relies on the safeguards these providers offer for international transfers, such as the European Commission's Standard Contractual Clauses or certification under the EU–US Data Privacy Framework.

## When data is shared

- With the service providers listed above, to run the service.
- With the people you connect with, limited to what you switch on.
- If the law requires it, for example in response to a valid legal request, and only as far as required.

We don't sell personal data and don't share it for advertising.

## How long we keep data

- **On your phone:** until you delete it, clear the app's data or uninstall the app.
- **Account and cloud backup:** for as long as you have an account. When you delete your account, your login, cloud backup, connections, shared status, invites and backed-up voice notes are deleted.
- **Entries you delete** while keeping your account are replaced in the backup by a "deleted" marker, so your other devices remove them too. The marker keeps only the date or identifier needed to sync, not the content, and it is removed when you delete your account. Deleted periods keep their start and end dates in the marker, because those dates are needed to sync correctly.
- **Backed-up voice notes** stay in storage until you delete your account, even if you clear the day they belong to.
- **Usage counters** for Ask expire automatically after about 26 hours.
- **Invite codes** stop working after 24 hours. They remain linked to your account until it is deleted.
- **Server logs and email delivery records** are kept by Cloudflare and Brevo according to their own limited retention periods.
- **Website enquiries** are kept in our support inbox for as long as needed to handle your request.

## How data is protected

- Encrypted on-device storage, as described above, and an optional app-lock PIN that is stored only as a salted hash.
- Encrypted connections (HTTPS/TLS) between the app, our server and our providers.
- Firestore security rules that let only your signed-in account read or write your backup. A trusted person can read only the status fields you allow.
- No secret keys in the app: AI, email and storage credentials stay on our server.
- Short-lived, signed links for voice-note uploads and downloads.
- App integrity checks and per-account usage limits on our server.
- Discreet notifications by default, so your lock screen shows no cycle words.

No system is perfectly secure, but we work to protect your data and will notify you and the authorities of a personal data breach where the law requires it.

## Your choices and rights

In the app you can:

- use Tuulit without an account;
- switch "Personal answers in Ask" on or off;
- control each trusted person's permissions;
- export your logs and pain entries as a CSV file;
- delete all data on your phone;
- delete your account and cloud backup.

Under the GDPR you also have the right to access your data, correct it, delete it, restrict or object to its processing, receive it in a portable format, and withdraw consent. To use these rights, email [ycomspacegh@gmail.com](mailto:ycomspacegh@gmail.com). We may need to confirm that the request comes from the account holder.

You also have the right to lodge a complaint with a data protection supervisory authority, in particular in the EU country where you live or work, or where an alleged infringement took place.

## Deleting your data

You can delete your account from the app (*You → Account and backup → Delete account*) or ask us by email. The [Account and data deletion](https://tuulit.app/account-deletion) page explains exactly what is deleted.

## Age

Tuulit is intended for people aged 16 and over. If you believe a child under 16 has given us personal data, contact us and we will delete it.

## Changes to this policy

When our data practices change, we will update this page and its version and "last updated" date. For significant changes we will also tell you in the app, or by email if you have an account, before the change takes effect.

## Contact

Ycomspace, Germany. Email: [ycomspacegh@gmail.com](mailto:ycomspacegh@gmail.com). You can also use our [contact page](https://tuulit.app/contact).
