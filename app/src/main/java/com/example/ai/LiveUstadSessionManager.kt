package com.example.ai

import android.annotation.SuppressLint
import android.util.Log
import com.example.model.CatalogData
import com.example.model.ServiceItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Transcription
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class LiveVoiceConnectionStatus {
    IDLE,
    CONNECTING,
    LIVE,
    ERROR
}

data class LiveTranscriptEntry(
    val id: String,
    val speaker: String, // "YOU" or "USTAD"
    val text: String
)

data class LiveVoiceUiState(
    val status: LiveVoiceConnectionStatus = LiveVoiceConnectionStatus.IDLE,
    val isAudioActive: Boolean = false,
    val transcript: List<LiveTranscriptEntry> = emptyList(),
    val recommendedService: ServiceItem? = null,
    val recommendationReason: String? = null,
    val errorMessage: String? = null
)

@OptIn(PublicPreviewAPI::class)
class LiveUstadSessionManager(
    private val city: String = "Lahore"
) {
    private val _state = MutableStateFlow(LiveVoiceUiState())
    val state: StateFlow<LiveVoiceUiState> = _state.asStateFlow()

    private var session: LiveSession? = null
    private var sessionJob: Job? = null

    private val recommendServiceTool = FunctionDeclaration(
        name = "recommend_kaamwala_service",
        description = "Recommend the best KaamWala home service category and open a booking shortcut for the user after diagnosing their issue.",
        parameters = mapOf(
            "serviceId" to Schema.enumeration(
                values = CatalogData.SERVICES.map { it.id },
                description = "Exact KaamWala service ID (e.g. ac, elec, plumb, solar, carp, paint, salon, clean, tutor, car, mech, errand)"
            ),
            "reason" to Schema.string(
                description = "Short 1-sentence explanation of why this service solves the customer's issue"
            )
        )
    )

    private val liveModel by lazy {
        val serviceSummary = CatalogData.SERVICES.joinToString("; ") {
            "${it.id}: ${it.nameEn} (Rs. ${it.fee})"
        }
        Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = LIVE_MODEL_NAME,
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                speechConfig = SpeechConfig(voice = Voice("AOEDE"))
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            tools = listOf(Tool.functionDeclarations(listOf(recommendServiceTool))),
            systemInstruction = content {
                text(
                    """
                    You are AI Ustad, the helpful, warm, practical home-services advisor for KaamWala in $city, Pakistan.
                    Respond naturally in concise spoken Urdu/Roman-Urdu or English, matching whichever language the user speaks.
                    Help diagnose AC faults, solar/inverter issues, electrical tripping, plumbing leaks, carpentry, painting, doorstep car wash, or salon/home care.
                    Give 1 safe immediate precaution tip and approximate KaamWala pricing.
                    Available services: $serviceSummary.
                    Whenever the user's need clearly maps to one of these services, call recommend_kaamwala_service with the matching serviceId and a brief reason so they can book with one tap.
                    Keep spoken replies crisp (under 3 sentences).
                    """.trimIndent()
                )
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startConversation(scope: CoroutineScope) {
        if (_state.value.status == LiveVoiceConnectionStatus.CONNECTING ||
            _state.value.status == LiveVoiceConnectionStatus.LIVE
        ) {
            return
        }

        sessionJob?.cancel()
        sessionJob = scope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    status = LiveVoiceConnectionStatus.CONNECTING,
                    errorMessage = null
                )
            }
            try {
                val activeSession = withTimeout(CONNECT_TIMEOUT_MS) {
                    liveModel.connect()
                }
                session = activeSession
                _state.update {
                    it.copy(
                        status = LiveVoiceConnectionStatus.LIVE,
                        isAudioActive = true,
                        errorMessage = null
                    )
                }

                activeSession.startAudioConversation(
                    ::handleFunctionCall,
                    ::handleTranscription,
                    true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Live API conversation error", e)
                _state.update {
                    it.copy(
                        status = LiveVoiceConnectionStatus.ERROR,
                        isAudioActive = false,
                        errorMessage = e.localizedMessage ?: "Live voice connection failed."
                    )
                }
            }
        }
    }

    fun sendQuickTopic(scope: CoroutineScope, prompt: String) {
        val activeSession = session ?: return
        scope.launch(Dispatchers.IO) {
            try {
                appendTranscript("YOU", prompt)
                activeSession.sendTextRealtime(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send realtime topic prompt", e)
                _state.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Could not send topic.")
                }
            }
        }
    }

    fun toggleMic(scope: CoroutineScope) {
        val activeSession = session ?: return
        scope.launch(Dispatchers.IO) {
            try {
                if (_state.value.isAudioActive) {
                    activeSession.stopAudioConversation()
                    _state.update { it.copy(isAudioActive = false) }
                } else {
                    @SuppressLint("MissingPermission")
                    activeSession.startAudioConversation(
                        ::handleFunctionCall,
                        ::handleTranscription,
                        true
                    )
                    _state.update { it.copy(isAudioActive = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed toggling live audio mic", e)
                _state.update {
                    it.copy(
                        status = LiveVoiceConnectionStatus.ERROR,
                        isAudioActive = false,
                        errorMessage = e.localizedMessage ?: "Audio toggle failed."
                    )
                }
            }
        }
    }

    fun stopConversation(scope: CoroutineScope) {
        sessionJob?.cancel()
        sessionJob = null
        val activeSession = session
        session = null
        scope.launch(Dispatchers.IO) {
            try {
                activeSession?.stopAudioConversation()
                activeSession?.close()
            } catch (_: Exception) {
            }
        }
        _state.update {
            it.copy(
                status = LiveVoiceConnectionStatus.IDLE,
                isAudioActive = false
            )
        }
    }

    private fun handleFunctionCall(call: FunctionCallPart): FunctionResponsePart {
        if (call.name == "recommend_kaamwala_service") {
            val serviceId = call.args["serviceId"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val reason = call.args["reason"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val matched = CatalogData.SERVICES.firstOrNull { it.id == serviceId }
            if (matched != null) {
                _state.update {
                    it.copy(
                        recommendedService = matched,
                        recommendationReason = reason.ifBlank { matched.desc }
                    )
                }
                return FunctionResponsePart(
                    name = call.name,
                    response = JsonObject(
                        mapOf(
                            "status" to JsonPrimitive("ok"),
                            "serviceName" to JsonPrimitive(matched.nameEn),
                            "visitFeePkr" to JsonPrimitive(matched.fee)
                        )
                    ),
                    id = call.id
                )
            }
        }
        return FunctionResponsePart(
            name = call.name,
            response = JsonObject(mapOf("status" to JsonPrimitive("unknown_function"))),
            id = call.id
        )
    }

    private fun handleTranscription(inputTx: Transcription?, outputTx: Transcription?) {
        val userText = inputTx?.text?.trim().orEmpty()
        if (userText.isNotEmpty()) {
            appendTranscript("YOU", userText)
        }
        val modelText = outputTx?.text?.trim().orEmpty()
        if (modelText.isNotEmpty()) {
            appendTranscript("USTAD", modelText)
        }
    }

    private fun appendTranscript(speaker: String, chunk: String) {
        _state.update { current ->
            val list = current.transcript.toMutableList()
            val last = list.lastOrNull()
            if (last != null && last.speaker == speaker) {
                val combined = if (last.text.endsWith(" ") || chunk.startsWith(" ")) {
                    last.text + chunk
                } else {
                    "${last.text} $chunk"
                }
                list[list.lastIndex] = last.copy(text = combined.trim())
            } else {
                list.add(
                    LiveTranscriptEntry(
                        id = "${speaker}_${System.nanoTime()}",
                        speaker = speaker,
                        text = chunk
                    )
                )
            }
            current.copy(transcript = list.takeLast(12))
        }
    }

    companion object {
        private const val TAG = "LiveUstadSession"
        private const val LIVE_MODEL_NAME = "gemini-3.1-flash-live-preview"
        private const val CONNECT_TIMEOUT_MS = 30_000L
    }
}
