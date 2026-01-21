package com.agentos.di

import com.agentos.claude.ClaudeApiClient
import com.agentos.ui.chat.ChatViewModel
import com.agentos.voice.VoicePipeline
import com.agentos.voice.WakeWordDetector
import com.agentos.workflow.WorkflowEngine
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // API Clients
    single { ClaudeApiClient() }

    // Workflow Engine
    single { WorkflowEngine(get()) }

    // Voice
    single { WakeWordDetector(androidContext()) }
    single { VoicePipeline(androidContext(), get(), get()) }

    // ViewModels
    viewModel { ChatViewModel(get(), get()) }
}
