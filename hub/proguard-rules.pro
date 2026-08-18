# CPZ Media Hub V1 intentionally has no reflection-based runtime integrations.
# Keep the launcher activity name stable for Android manifest resolution.
-keep class com.cpozom.mediahub.HubActivity { *; }
