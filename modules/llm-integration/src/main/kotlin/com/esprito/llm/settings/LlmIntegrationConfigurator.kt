package com.esprito.llm.settings

import com.esprito.llm.LlmBundle
import com.esprito.llm.service.LlmHttpExplytService
import com.explyt.ai.backend.http.AvailableOllamaModelsRequest
import com.explyt.ai.backend.http.ModelInfo
import com.explyt.ai.backend.http.Provider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JComponent

// see GithubShareDialog
class LlmIntegrationConfigurator() : SearchableConfigurable {
    private lateinit var panel: DialogPanel
    private val instance = LlmIntegrationSettingsState.getInstance()
    private val aiBackendLocal = LlmHttpExplytService.getInstance().aiBackendLocal
    private val providers =
        listOf(Provider.OpenAI, Provider.Ollama, Provider.Mistral, Provider.Anthropic, Provider.DeepSeek)

    private val propertyGraph = PropertyGraph()
    private val apiKeyProperty = propertyGraph.property("")
    private val ollamaUrlProperty = propertyGraph.property("")
    private val providerComboBoxModel = CollectionComboBoxModel(providers)
    private val llmModelComboBoxModel = CollectionComboBoxModel(mutableListOf<String>())
    private val ollamaVisible = AtomicBooleanProperty(false)

    override fun createComponent(): JComponent {
        panel = panel {
            group(LlmBundle.message("explyt.llm.settings.header")) {
                row(LlmBundle.message("explyt.llm.settings.provider.title")) {
                    comboBox(providerComboBoxModel)
                        .align(AlignX.FILL)
                        .applyToComponent { addActionListener { changeProvider() } }
                        .resizableColumn()
                }

                row(LlmBundle.message("explyt.llm.settings.ollama.url.header")) {
                    textField()
                        .align(AlignX.FILL)
                        .applyToComponent {
                            emptyText.text = LlmBundle.message("explyt.llm.settings.ollama.url.placeholder")
                            text = instance.ollamaUrl
                        }
                        .bindText(ollamaUrlProperty)
                        .resizableColumn()

                    actionButton(object : AnAction() {
                        override fun getActionUpdateThread() = ActionUpdateThread.BGT

                        override fun actionPerformed(e: AnActionEvent) {
                            ollamaModelInfos()
                        }
                    }).applyToComponent {
                        presentation.text = LlmBundle.message("explyt.llm.settings.ollama.button.model.list")
                        presentation.icon = AllIcons.Actions.Refresh
                    }.align(AlignX.RIGHT)
                }.visibleIf(ollamaVisible)

                row(LlmBundle.message("explyt.llm.settings.model.title")) {
                    comboBox(llmModelComboBoxModel)
                        .align(AlignX.FILL)
                        .validationOnApply { validateModel() }
                        .resizableColumn()
                }

                row(LlmBundle.message("explyt.llm.settings.api.key.title")) {
                    textField()
                        .align(AlignX.FILL)
                        .applyToComponent {
                            emptyText.text = LlmBundle.message("explyt.llm.settings.api.key.placeholder")
                            text = instance.apiKey
                        }
                        .bindText(apiKeyProperty)
                        .resizableColumn()
                }.visibleIf(ollamaVisible.not())
            }
        }
        return panel
    }

    private fun changeProvider() {
        val llmProvider = providerComboBoxModel.selected ?: return
        ollamaVisible.set(llmProvider == Provider.Ollama)
        llmModelComboBoxModel.removeAll()
        setModelInfos(llmProvider)
    }

    private fun setModelInfos(llmProvider: Provider) {
        if (llmProvider == Provider.Ollama) {
            llmModelComboBoxModel.selectedItem = ""
            ollamaModelInfos()
        } else {
            val modelInfos = otherSimpleModelInfos(llmProvider)
            updateModelInfos(modelInfos)
        }
    }

    private fun otherSimpleModelInfos(llmProvider: Provider): List<ModelInfo> {
        return aiBackendLocal.availableModels().providerToModelConfigs[llmProvider] ?: emptyList()
    }

    private fun ollamaModelInfos() {
        CoroutineScope(Dispatchers.IO).launch {
            val models = aiBackendLocal.availableOllamaModels(AvailableOllamaModelsRequest(ollamaUrlProperty.get()))
            ApplicationManager.getApplication().invokeLater {
                updateModelInfos(models.models)
            }
        }
    }

    private fun updateModelInfos(modelInfos: List<ModelInfo>) {
        val models = modelInfos.map { it.modelName }
        models.forEach { llmModelComboBoxModel.add(it) }
        llmModelComboBoxModel.selectedItem = models.firstOrNull()
        llmModelComboBoxModel.update()
    }

    override fun isModified(): Boolean {
        if (instance.provider != providerComboBoxModel.selected?.name) return true
        if (instance.providerModel != llmModelComboBoxModel.selected) return true
        if ((instance.apiKey ?: "") != apiKeyProperty.get()) return true
        if ((instance.ollamaUrl ?: "") != ollamaUrlProperty.get()) return true
        return false
    }

    override fun reset() {
        val provider = providers.find { it.name == instance.provider } ?: Provider.DeepSeek
        providerComboBoxModel.selectedItem = provider
        providerComboBoxModel.update()
        changeProvider()
        llmModelComboBoxModel.selectedItem = instance.providerModel
        apiKeyProperty.set(instance.apiKey ?: "")
        ollamaUrlProperty.set(instance.ollamaUrl ?: "")
    }

    override fun apply() {
        instance.provider = providerComboBoxModel.selected?.name ?: throw Exception()
        instance.providerModel = llmModelComboBoxModel.selected ?: throw Exception()
        instance.apiKey = apiKeyProperty.get()
        instance.ollamaUrl = ollamaUrlProperty.get()
    }

    override fun getDisplayName() = LlmBundle.message("explyt.llm.settings.header.title")

    override fun getId(): String {
        return javaClass.name
    }

    private fun ValidationInfoBuilder.validateModel(): ValidationInfo? {
        val provider = providerComboBoxModel.selected ?: return null
        if (provider == Provider.Ollama) return null

        return if (llmModelComboBoxModel.selected == null) {
            error("Model is empty")
        } else {
            val model = llmModelComboBoxModel.selected
            val modelInfo = aiBackendLocal.availableModels().providerToModelConfigs[provider]
                ?.find { it.modelName == model }
            if (modelInfo == null) error("It is not provider model") else null
        }
    }
}

