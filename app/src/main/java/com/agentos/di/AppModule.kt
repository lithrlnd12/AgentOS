package com.agentos.di

import com.agentos.claude.ClaudeApiClient
import com.agentos.workflow.WorkflowEngine
import org.koin.dsl.module

val appModule = module {
    // API Clients
    single { ClaudeApiClient() }

    // Workflow Engine
    single { WorkflowEngine(get()) }

    // ViewModels will be added here
    // viewModel { ChatViewModel(get()) }
}
