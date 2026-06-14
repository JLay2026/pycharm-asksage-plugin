package asksage.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.Messages
import asksage.api.AskSageApiClient
import asksage.api.AskSageApiException
import asksage.api.auth.AuthManager
import asksage.api.models.TrainRequest
import asksage.services.AskSageSettingsState
import asksage.services.DatasetRegistryService
import asksage.services.EditorContextService
import asksage.util.NotificationHelper

class AddToKnowledgeBaseAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val context = EditorContextService.getEditorContext(project)
        if (context == null) {
            Messages.showWarningDialog(project, "No file is currently open in the editor.", "AskSage")
            return
        }

        val settings = AskSageSettingsState.getInstance()
        val datasetRegistry = DatasetRegistryService.getInstance()
        val datasets = datasetRegistry.getDatasets()

        val datasetOptions = if (datasets.isNotEmpty()) {
            datasets.toTypedArray()
        } else {
            arrayOf("default")
        }

        val selectedDataset = if (datasetOptions.size == 1) {
            datasetOptions[0]
        } else {
            Messages.showEditableChooseDialog(
                "Select the dataset to add this content to:",
                "Add to Knowledge Base",
                null,
                datasetOptions,
                settings.defaultDataset.ifBlank { datasetOptions[0] },
                null,
            ) ?: return
        }

        val contentToTrain = if (context.selectedText != null) {
            context.selectedText
        } else {
            context.fullContent
        }

        val title = Messages.showInputDialog(
            project,
            "Title for this knowledge entry (optional):",
            "Add to Knowledge Base",
            null,
            context.fileName,
            null,
        )

        if (title == null) return

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val apiClient = AskSageApiClient(settings.baseUrl)
                val authManager = AuthManager.getInstance()
                val token = authManager.getAccessToken(apiClient)
                    ?: throw AskSageApiException("Authentication failed")

                // The Ask Sage /server/train endpoint takes `content` and an optional
                // `force_dataset`; the optional title is folded into `context`.
                val request = TrainRequest(
                    content = contentToTrain,
                    context = title.ifBlank { context.fileName },
                    forceDataset = selectedDataset,
                )

                val response = apiClient.train(token, request)
                val result = response.response ?: "Content added"

                ApplicationManager.getApplication().invokeLater {
                    Messages.showInfoMessage(
                        project,
                        "Successfully added to dataset '$selectedDataset':\n$result",
                        "AskSage - Knowledge Base",
                    )
                }
            } catch (e: AskSageApiException) {
                LOG.warn("Failed to add to knowledge base", e)
                NotificationHelper.error(project, "Knowledge Base Error", "Failed to add to knowledge base: ${e.message}")
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to add to knowledge base: ${e.message}",
                        "AskSage Error",
                    )
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }

    companion object {
        private val LOG = logger<AddToKnowledgeBaseAction>()
    }
}
