document.addEventListener("DOMContentLoaded", () => {
    // Elements
    const historyList = document.getElementById("historyList");
    const modelBadge = document.getElementById("modelBadge");
    const chatWindow = document.getElementById("chatWindow");
    const queryInput = document.getElementById("queryInput");
    const sendBtn = document.getElementById("sendBtn");
    const modelToggle = document.getElementById("modelToggle");
    const attachmentBtn = document.getElementById("attachmentBtn");
    const fileInput = document.getElementById("fileInput");
    const filePreviewContainer = document.getElementById("filePreviewContainer");
    const micBtn = document.getElementById("micBtn");
    const exportChatBtn = document.getElementById("exportChatBtn");

    // --- Sidebar History ---
    const loadHistory = async () => {
        try {
            const response = await fetch("/api/nexus/history");
            const data = await response.json();
            if (data.length > 0) {
                historyList.innerHTML = "";
                data.slice(-10).reverse().forEach(log => {
                    const item = document.createElement("div");
                    item.className = "history-item";
                    item.textContent = log.userQuery;
                    item.title = log.userQuery;
                    item.onclick = () => {
                        queryInput.value = log.userQuery;
                        handleSend();
                    };
                    historyList.appendChild(item);
                });
            }
        } catch (e) {
            console.error("Failed to load history", e);
        }
    };

    loadHistory();

    // --- Model Selection ---
    modelToggle.addEventListener("change", (e) => {
        const val = e.target.value;
        if (val === 'openai') {
            modelBadge.textContent = 'GPT-4 Turbo';
            modelBadge.style.background = 'rgba(99, 102, 241, 0.2)';
            modelBadge.style.color = '#6366f1';
        } else if (val === 'gemini') {
            modelBadge.textContent = 'Google Gemini';
            modelBadge.style.background = 'rgba(52, 168, 83, 0.2)';
            modelBadge.style.color = '#34a853';
        } else {
            modelBadge.textContent = 'Ollama (LLaMA-3)';
            modelBadge.style.background = 'rgba(168, 85, 247, 0.2)';
            modelBadge.style.color = '#a855f7';
        }
    });

    // --- Voice Input (Speech-to-Text) ---
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
        const recognition = new SpeechRecognition();
        recognition.continuous = false;
        recognition.lang = 'en-US';

        recognition.onstart = () => {
            micBtn.classList.add("mic-active");
            queryInput.placeholder = "Listening...";
        };

        recognition.onresult = (event) => {
            const transcript = event.results[0][0].transcript;
            queryInput.value = transcript;
            handleSend();
        };

        recognition.onend = () => {
            micBtn.classList.remove("mic-active");
            queryInput.placeholder = "Enter your question or upload a file...";
        };

        micBtn.addEventListener("click", () => {
            recognition.start();
        });
    } else {
        micBtn.style.display = "none";
    }

    // --- Voice Output (Text-to-Speech) ---
    const speakText = (text) => {
        // Strip HTML tags for clean speech
        const tempDiv = document.createElement("div");
        tempDiv.innerHTML = text;
        const cleanText = tempDiv.textContent || tempDiv.innerText || "";
        
        const utterance = new SpeechSynthesisUtterance(cleanText);
        utterance.rate = 1.0;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
    };

    // --- Export Chat (Markdown) ---
    exportChatBtn.addEventListener("click", () => {
        let exportStr = "# Nexus AI Chat Export\n\n";
        const messages = chatWindow.querySelectorAll('.message');
        
        messages.forEach(msg => {
            const isUser = msg.classList.contains('user');
            const contentDiv = msg.querySelector('.msg-content');
            
            // Reconstruct basic formatting for export
            const textToExport = contentDiv.innerText;
            
            if (isUser) {
                exportStr += `**User:** ${textToExport}\n\n`;
            } else {
                exportStr += `**Nexus AI:**\n${textToExport}\n\n---\n\n`;
            }
        });

        const blob = new Blob([exportStr], { type: 'text/markdown' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Nexus_Chat_${new Date().toISOString().slice(0,10)}.md`;
        a.click();
        URL.revokeObjectURL(url);
    });

    // --- File Attachments ---
    attachmentBtn.addEventListener("click", () => {
        fileInput.click();
    });

    fileInput.addEventListener("change", () => {
        filePreviewContainer.innerHTML = "";
        Array.from(fileInput.files).forEach(file => {
            const badge = document.createElement("span");
            badge.className = "file-badge";
            badge.textContent = file.name;
            filePreviewContainer.appendChild(badge);
            
            if (modelToggle.value === 'ollama' && file.type.startsWith('image')) {
                const warn = document.createElement("span");
                warn.className = "file-badge file-warning";
                warn.textContent = "Warning: Ollama ignores images";
                filePreviewContainer.appendChild(warn);
            }
        });
    });

    const dragOverlay = document.getElementById("dragOverlay");

    window.addEventListener("dragover", (e) => {
        e.preventDefault();
        dragOverlay.classList.add("active");
    });

    window.addEventListener("dragleave", (e) => {
        e.preventDefault();
        if (e.relatedTarget === null) {
            dragOverlay.classList.remove("active");
        }
    });

    window.addEventListener("drop", (e) => {
        e.preventDefault();
        dragOverlay.classList.remove("active");
        
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            fileInput.files = e.dataTransfer.files;
            const event = new Event('change');
            fileInput.dispatchEvent(event);
        }
    });

    // --- Chat Logic ---
    const addMessage = (content, sender) => {
        const msgDiv = document.createElement("div");
        msgDiv.className = `message ${sender}`;
        let avatar = sender === 'user' ? 'U' : 'N';
        msgDiv.innerHTML = `
            <div class="avatar">${avatar}</div>
            <div class="msg-content">${content}</div>
        `;
        chatWindow.appendChild(msgDiv);
        chatWindow.scrollTop = chatWindow.scrollHeight;
        return msgDiv.querySelector('.msg-content');
    };

    const handleSend = async () => {
        const text = queryInput.value.trim();
        const files = fileInput.files;
        
        if (!text && files.length === 0) return;

        let displayHtml = text;
        if (files.length > 0) {
            displayHtml += `<br><small><i>[Attached ${files.length} file(s)]</i></small>`;
        }

        addMessage(displayHtml, "user");
        queryInput.value = "";
        
        // Typing indicator
        const msgWrapper = document.createElement("div");
        msgWrapper.className = `message bot`;
        msgWrapper.innerHTML = `
            <div class="avatar">N</div>
            <div class="msg-content"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div>
        `;
        chatWindow.appendChild(msgWrapper);
        chatWindow.scrollTop = chatWindow.scrollHeight;
        
        const botMsgContent = msgWrapper.querySelector('.msg-content');
        
        const model = modelToggle.value;
        const formData = new FormData();
        formData.append("query", text);
        formData.append("model", model);
        for (let i = 0; i < files.length; i++) {
            formData.append("files", files[i]);
        }

        fileInput.value = "";
        filePreviewContainer.innerHTML = "";

        try {
            const response = await fetch('/api/nexus/chat', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) throw new Error("Network error");
            
            botMsgContent.innerHTML = ""; // Clear dots
            let fullText = "";

            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = "";

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    // Triggers when stream fully completes securely
                    if (window.hljs) {
                        botMsgContent.querySelectorAll('pre code').forEach((block) => {
                            hljs.highlightElement(block);
                        });
                    }
                    
                    // Inject TTS Read Button
                    const speakBtn = document.createElement("button");
                    speakBtn.className = "speaker-btn";
                    speakBtn.innerHTML = "🔊";
                    speakBtn.onclick = () => speakText(fullText);
                    botMsgContent.parentElement.append(speakBtn);
                    
                    break;
                }
                
                buffer += decoder.decode(value, { stream: true });
                const events = buffer.split('\n\n');
                buffer = events.pop(); // keep the last incomplete chunk

                for (const event of events) {
                    if (event.startsWith('data:')) {
                        const dataStr = event.slice(5).trim();
                        if (dataStr) {
                            try {
                                const data = JSON.parse(dataStr);
                                if (data.type === "token") {
                                    fullText += data.content;
                                    
                                    // Parse Markdown cleanly
                                    let formattedText = fullText
                                        .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
                                        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                                        .replace(/\*(.*?)\*/g, '<em>$1</em>')
                                        .replace(/^### (.*$)/gim, '<h3>$1</h3>')
                                        .replace(/^\* (.*$)/gim, '<li>$1</li>')
                                        .replace(/\n/g, '<br>');

                                    botMsgContent.innerHTML = formattedText;
                                    chatWindow.scrollTop = chatWindow.scrollHeight;
                                } else if (data.type === "error") {
                                    botMsgContent.innerHTML = "Error: " + data.content;
                                }
                            } catch (e) {}
                        }
                    }
                }
            }
        } catch (error) {
            botMsgContent.innerHTML = "Network error or connection dropped.";
        }
    };

    sendBtn.addEventListener("click", handleSend);
    queryInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") handleSend();
    });
});
