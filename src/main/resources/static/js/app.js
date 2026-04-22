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
    const quickRepliesContainer = document.getElementById("quickRepliesContainer");
    const micBtn = document.getElementById("micBtn");
    const cameraBtn = document.getElementById("cameraBtn");
    const exportChatBtn = document.getElementById("exportChatBtn");
    const printPdfBtn = document.getElementById("printPdfBtn");
    const clearMemoryBtn = document.getElementById("clearMemoryBtn");
    const themeSwitch = document.getElementById("checkboxTheme");
    const logoutBtn = document.getElementById("logoutBtn");

    // --- User Session ---
    const sessionData = JSON.parse(localStorage.getItem("nexus_user") || "{}");
    if (sessionData.name) {
        document.getElementById("userName").textContent = sessionData.name;
        document.getElementById("userRole").textContent = sessionData.role;
        document.getElementById("userAvatar").textContent = sessionData.name.charAt(0).toUpperCase();
    }

    logoutBtn.addEventListener("click", () => {
        if (confirm("Are you sure you want to logout?")) {
            localStorage.removeItem("nexus_user");
            window.location.href = "login.html";
        }
    });

    // --- Theme Engine ---
    const currentTheme = localStorage.getItem("theme");
    if (currentTheme === "light") {
        document.body.classList.add("light-mode");
        themeSwitch.checked = true;
    }
    themeSwitch.addEventListener("change", (e) => {
        if (e.target.checked) {
            document.body.classList.add("light-mode");
            localStorage.setItem("theme", "light");
        } else {
            document.body.classList.remove("light-mode");
            localStorage.setItem("theme", "dark");
        }
    });

    // --- Dynamic Textarea ---
    queryInput.addEventListener("input", function() {
        this.style.height = "auto";
        this.style.height = (this.scrollHeight) + "px";
        if (this.value === "") {
            this.style.height = "auto";
        }
    });

    // --- Clear Session ---
    clearMemoryBtn.addEventListener("click", async () => {
        if(confirm("Are you sure you want to clear the AI's memory and chat history?")) {
            try {
                await fetch('/api/nexus/memory', { method: 'DELETE' });
                chatWindow.innerHTML = `
                    <div class="message bot">
                        <div class="avatar">N</div>
                        <div class="msg-content">
                            Memory purged! Starting a fresh session. How can I help you today?
                        </div>
                    </div>`;
            } catch(e) {
                console.error("Failed to clear memory");
            }
        }
    });

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
        } else if (val === 'groq') {
            modelBadge.textContent = 'Groq (LLaMA-3.3 70B)';
            modelBadge.style.background = 'rgba(245, 158, 11, 0.2)';
            modelBadge.style.color = '#f59e0b';
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

    // --- PDF Export ---
    printPdfBtn.addEventListener("click", () => {
        window.print();
    });

    // --- Webcam Integration ---
    cameraBtn.addEventListener("click", async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true });
            const video = document.createElement("video");
            video.style.position = "fixed";
            video.style.top = "-9999px";
            video.autoplay = true;
            video.playsInline = true;
            video.srcObject = stream;
            document.body.appendChild(video);
            
            // Wait for video data to fully paint to DOM AND let exposure adjust
            await new Promise((resolve) => {
                video.onplaying = () => {
                    setTimeout(resolve, 800);
                };
            });

            // Create hidden canvas to capture frame
            const canvas = document.createElement("canvas");
            canvas.width = video.videoWidth || 640;
            canvas.height = video.videoHeight || 480;
            const ctx = canvas.getContext("2d");
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

            // Stop webcam immediately after snap and cleanup DOM
            stream.getTracks().forEach(track => track.stop());
            document.body.removeChild(video);

            canvas.toBlob((blob) => {
                const file = new File([blob], `Webcam_${Date.now()}.png`, { type: "image/png" });
                
                // Add to standard file input via DataTransfer
                const dataTransfer = new DataTransfer();
                for (let i = 0; i < fileInput.files.length; i++) {
                    dataTransfer.items.add(fileInput.files[i]);
                }
                dataTransfer.items.add(file);
                fileInput.files = dataTransfer.files;
                
                // Manually trigger preview update
                const event = new Event('change');
                fileInput.dispatchEvent(event);
            }, 'image/png');

        } catch (err) {
            console.error("Camera error:", err);
            alert("Could not access webcam. Please check permissions!");
        }
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

    // --- Quick Replies System ---
    const showQuickReplies = () => {
        quickRepliesContainer.innerHTML = "";
        const prompts = [
            "Explain this simpler",
            "Give me a code example",
            "Summarize this"
        ];
        prompts.forEach(p => {
            const pill = document.createElement("button");
            pill.className = "quick-reply-pill";
            pill.textContent = p;
            pill.onclick = () => {
                queryInput.value = p;
                handleSend();
            };
            quickRepliesContainer.appendChild(pill);
        });
    };

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
        const persona = document.getElementById("personaToggle").value;
        
        // Inject persona silently if not standard
        let finalQuery = text;
        if (persona !== "Standard Assistant") {
            finalQuery = `[SYSTEM ATTENTION: ACT STRICTLY AS THE FOLLOWING PERSONA: "${persona}"]\n\n${text}`;
        }

        const formData = new FormData();
        formData.append("query", finalQuery);
        formData.append("model", model);
        for (let i = 0; i < files.length; i++) {
            formData.append("files", files[i]);
        }

        fileInput.value = "";
        filePreviewContainer.innerHTML = "";

        const startTime = Date.now();
        quickRepliesContainer.innerHTML = ""; // Hide pills during generation

        // Stream Killing Abort Logic
        const abortController = new AbortController();
        const originalSendHtml = sendBtn.innerHTML;
        sendBtn.innerHTML = "🛑";
        sendBtn.classList.add("stop-btn-active");
        sendBtn.title = "Stop Generation";
        
        const stopHandler = (e) => {
            e.stopPropagation();
            abortController.abort();
        };
        sendBtn.removeEventListener("click", handleSend);
        sendBtn.addEventListener("click", stopHandler);

        try {
            const response = await fetch('/api/nexus/chat', {
                method: 'POST',
                body: formData,
                signal: abortController.signal
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
                    if (window.hljs) {
                        botMsgContent.querySelectorAll('pre code').forEach((block) => {
                            // Wrapper for positioning
                            const wrapper = document.createElement('div');
                            wrapper.className = 'code-wrapper';
                            block.parentNode.insertBefore(wrapper, block);
                            wrapper.appendChild(block);
                            
                            // Copy Code button
                            const copyBtn = document.createElement('button');
                            copyBtn.className = 'copy-code-btn';
                            copyBtn.textContent = 'Copy';
                            copyBtn.onclick = () => {
                                navigator.clipboard.writeText(block.innerText);
                                copyBtn.textContent = 'Copied!';
                                setTimeout(() => copyBtn.textContent = 'Copy', 2000);
                            };
                            wrapper.appendChild(copyBtn);
                            
                            hljs.highlightElement(block);
                        });
                    }
                    
                    // Inject TTS Read & Copy Buttons
                    const speakBtn = document.createElement("button");
                    speakBtn.className = "speaker-btn";
                    speakBtn.innerHTML = "🔊";
                    speakBtn.title = "Read Aloud";
                    speakBtn.onclick = () => speakText(fullText);

                    const copyGlobalBtn = document.createElement("button");
                    copyGlobalBtn.className = "copy-btn";
                    copyGlobalBtn.innerHTML = "📋";
                    copyGlobalBtn.title = "Copy Text";
                    copyGlobalBtn.onclick = () => {
                        navigator.clipboard.writeText(fullText);
                        copyGlobalBtn.innerHTML = "✅";
                        setTimeout(() => copyGlobalBtn.innerHTML = "📋", 2000);
                    };

                    botMsgContent.parentElement.append(speakBtn);
                    botMsgContent.parentElement.append(copyGlobalBtn);
                    
                    // Telemetry Logic
                    const processTime = ((Date.now() - startTime) / 1000).toFixed(2);
                    const telemetryBadge = document.createElement("span");
                    telemetryBadge.className = "telemetry-badge";
                    telemetryBadge.innerHTML = `⚡ Generated in ${processTime}s`;
                    botMsgContent.parentElement.append(telemetryBadge);
                    showQuickReplies();

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
            if (error.name === 'AbortError') {
                botMsgContent.innerHTML += "<br><span style='color:#ef4444'>[Generation Aborted by User]</span>";
                showQuickReplies();
            } else {
                botMsgContent.innerHTML = "Network error or connection dropped.";
            }
        } finally {
            // Restore Send Button
            sendBtn.removeEventListener("click", stopHandler);
            sendBtn.addEventListener("click", handleSend);
            sendBtn.innerHTML = originalSendHtml;
            sendBtn.classList.remove("stop-btn-active");
            sendBtn.title = "Send";
        }
    };

    sendBtn.addEventListener("click", handleSend);
    queryInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    });
});
