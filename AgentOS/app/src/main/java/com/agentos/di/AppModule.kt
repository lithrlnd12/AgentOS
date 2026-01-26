package com.agentos.di

import android.content.Context
import com.agentos.BuildConfig
import com.agentos.ai.ConversationEngine
import com.agentos.ai.IntentClassifier
import com.agentos.claude.ClaudeApiClient
import com.agentos.core.logging.Logger
import com.agentos.core.monitoring.PerformanceMonitor
import com.agentos.core.apps.AppManager
import com.agentos.data.ConversationRepository
import com.agentos.ui.chat.ChatViewModel
import com.agentos.voice.AudioPlayer
import com.agentos.voice.OpenAIVoiceManager
import com.agentos.voice.TextToSpeechEngine
import com.agentos.voice.VoicePipeline
import com.agentos.voice.WakeWordDetector
import com.agentos.workflow.WorkflowEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLogger(): Logger {
        return Logger()
    }

    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor {
        return PerformanceMonitor()
    }

    @Provides
    @Singleton
    fun provideClaudeApiClient(): ClaudeApiClient {
        return ClaudeApiClient()
    }

    @Provides
    @Singleton
    fun provideAppManager(@ApplicationContext context: Context): AppManager {
        return AppManager(context)
    }

    @Provides
    @Singleton
    fun provideWorkflowEngine(
        claudeApiClient: ClaudeApiClient,
        appManager: AppManager
    ): WorkflowEngine {
        return WorkflowEngine(claudeApiClient, appManager)
    }

    @Provides
    @Singleton
    fun provideWakeWordDetector(@ApplicationContext context: Context): WakeWordDetector {
        return WakeWordDetector(context)
    }

    @Provides
    @Singleton
    fun provideTextToSpeechEngine(@ApplicationContext context: Context): TextToSpeechEngine {
        return TextToSpeechEngine(context)
    }

    @Provides
    @Singleton
    fun provideVoicePipeline(
        @ApplicationContext context: Context,
        workflowEngine: WorkflowEngine,
        wakeWordDetector: WakeWordDetector,
        ttsEngine: TextToSpeechEngine
    ): VoicePipeline {
        return VoicePipeline(context, workflowEngine, wakeWordDetector, ttsEngine)
    }

    @Provides
    @Singleton
    fun provideIntentClassifier(): IntentClassifier {
        return IntentClassifier()
    }

    @Provides
    @Singleton
    fun provideConversationEngine(claudeApiClient: ClaudeApiClient): ConversationEngine {
        return ConversationEngine(claudeApiClient)
    }

    @Provides
    @Singleton
    fun provideConversationRepository(@ApplicationContext context: Context): ConversationRepository {
        return ConversationRepository(context)
    }

    @Provides
    @Singleton
    fun provideAudioPlayer(): AudioPlayer {
        return AudioPlayer()
    }

    @Provides
    @Singleton
    fun provideOpenAIVoiceManager(
        @ApplicationContext context: Context,
        workflowEngine: WorkflowEngine
    ): OpenAIVoiceManager {
        return OpenAIVoiceManager(
            context = context,
            apiKey = BuildConfig.OPENAI_API_KEY,
            workflowEngine = workflowEngine
        )
    }

    @Provides
    @Singleton
    fun provideChatViewModel(
        workflowEngine: WorkflowEngine,
        voicePipeline: VoicePipeline,
        openAIVoiceManager: OpenAIVoiceManager,
        intentClassifier: IntentClassifier,
        conversationEngine: ConversationEngine,
        conversationRepository: ConversationRepository
    ): ChatViewModel {
        return ChatViewModel(
            workflowEngine = workflowEngine,
            voicePipeline = voicePipeline,
            openAIVoiceManager = openAIVoiceManager,
            intentClassifier = intentClassifier,
            conversationEngine = conversationEngine,
            conversationRepository = conversationRepository
        )
    }
}

/**
 * EntryPoint interface for accessing ChatViewModel in Service context.
 * Hilt doesn't automatically integrate with Services for ViewModel,
 * so we use this pattern to manually inject the singleton ChatViewModel.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FloatingAgentServiceEntryPoint {
    fun chatViewModel(): ChatViewModel
}
