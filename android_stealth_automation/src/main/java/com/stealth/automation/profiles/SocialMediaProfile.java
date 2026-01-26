package com.stealth.automation.profiles;

public class SocialMediaProfile extends AppBehaviorProfile {
    
    public SocialMediaProfile() {
        super("com.instagram.android", "Instagram");
    }
    
    @Override
    protected void initializeProfile() {
        // Instagram-specific behavior patterns
        behaviorParams.put("typing_speed", 220); // Faster typing for social media
        behaviorParams.put("swipe_velocity", 1.2); // Quick swipes for stories
        behaviorParams.put("scroll_frequency", 8); // High scrolling activity
        behaviorParams.put("error_rate", 0.015); // Lower error rate (familiar app)
        behaviorParams.put("simulate_reading", true);
        behaviorParams.put("session_duration", 600000L); // 10 min sessions
        behaviorParams.put("story_watch_time", 3000); // 3 seconds per story
        behaviorParams.put("post_interaction_rate", 0.3); // Like 30% of posts
        behaviorParams.put("comment_frequency", 0.1); // Comment on 10% of posts
        behaviorParams.put("hashtag_search_frequency", 0.4); // Search hashtags 40% of time
        behaviorParams.put("profile_visit_rate", 0.25); // Visit profiles 25% of time
    }
}
