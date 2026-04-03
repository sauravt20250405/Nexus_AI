document.addEventListener("DOMContentLoaded", () => {
    // Elements
    const pdfUpload = document.getElementById("pdfUpload");
    const uploadBtnLabel = document.getElementById("uploadBtn");
    const uploadStatus = document.getElementById("uploadStatus");
    const ingestBtn = document.getElementById("ingestBtn");
    
    const chatWindow = document.getElementById("chatWindow");
    const queryInput = document.getElementById("queryInput");
    const sendBtn = document.getElementById("sendBtn");
    const modelToggle = document.getElementById("modelToggle");
    const historyList = document.getElementById("historyList");
    const modelBadge = document.getElementById("modelBadge");

    let selectedFile = null;

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
        modelBadge.textContent = val === 'openai' ? 'GPT-4 Turbo' : 'Ollama (LLaMA-3)';
        modelBadge.style.background = val === 'openai' ? 'rgba(99, 102, 241, 0.2)' : 'rgba(168, 85, 247, 0.2)';
        modelBadge.style.color = val === 'openai' ? '#6366f1' : '#a855f7';
    });

    // --- Upload Logic ---
    pdfUpload.addEventListener("change", (e) => {
        if (e.target.files.length > 0) {
            selectedFile = e.target.files[0];
            uploadBtnLabel.textContent = selectedFile.name;
            uploadBtnLabel.style.borderColor = "#6366f1";
            ingestBtn.disabled = false;
            uploadStatus.textContent = "Ready to ingest.";
        }
    });

    ingestBtn.addEventListener("click", async () => {
        if (!selectedFile) return;
        const formData = new FormData();
        formData.append("file", selectedFile);
        ingestBtn.disabled = true;
        ingestBtn.textContent = "Ingesting...";
        uploadStatus.textContent = "Indexing...";

        try {
            const response = await fetch("/api/nexus/ingest", { method: "POST", body: formData });
            if (response.ok) {
                uploadStatus.textContent = "Success! Index updated.";
                uploadStatus.style.color = "#10b981";
                setTimeout(() => {
                    selectedFile = null;
                    pdfUpload.value = "";
                    uploadBtnLabel.textContent = "Choose PDF";
                    uploadStatus.textContent = "";
                    ingestBtn.textContent = "Ingest to Vector DB";
                }, 3000);
            }
        } catch (error) {
            uploadStatus.textContent = "Upload failed!";
            uploadStatus.style.color = "#ef4444";
            ingestBtn.disabled = false;
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
        if (!text) return;

        addMessage(text, "user");
        queryInput.value = "";
        
        // Typing indicator
        const botMsgContent = addMessage('<div class="dot"></div><div class="dot"></div><div class="dot"></div>', "bot");
        botMsgContent.innerHTML = ""; // Clear dots when stream starts

        let fullText = "";
        const model = modelToggle.value;

        try {
            const eventSource = new EventSource(`/api/nexus/chat?query=${encodeURIComponent(text)}&model=${model}`);
            
            eventSource.onmessage = (event) => {
                const data = JSON.parse(event.data);
                
                if (data.type === "token") {
                    // Smooth character typing effect
                    const token = data.content;
                    fullText += token;
                    botMsgContent.innerHTML = fullText.replace(/\n/g, '<br>');
                    chatWindow.scrollTop = chatWindow.scrollHeight;
                } else if (data.type === "citations") {
                    renderCitations(botMsgContent.parentElement, data.content);
                    eventSource.close();
                    loadHistory();
                } else if (data.type === "error") {
                    botMsgContent.innerHTML = "Error: " + data.content;
                    eventSource.close();
                }
            };

            eventSource.onerror = () => {
                eventSource.close();
                if (botMsgContent.innerHTML === "") {
                    botMsgContent.innerHTML = "Connection lost. Please try again.";
                }
            };
        } catch (error) {
            botMsgContent.innerHTML = "Network error.";
        }
    };

    const renderCitations = (messageDiv, citations) => {
        if (!citations || citations.length === 0) return;
        
        const container = document.createElement("div");
        container.className = "citations-container";
        
        // Use a Set to avoid duplicate filename/page combinations
        const uniqueKeys = new Set();
        
        citations.forEach(source => {
            const fileName = source.source_name || "Unknown Document";
            const page = source.page_number || "?";
            const key = `${fileName}_${page}`;
            
            if (!uniqueKeys.has(key)) {
                uniqueKeys.add(key);
                const badge = document.createElement("div");
                badge.className = "citation-badge";
                badge.innerHTML = `
                    <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
                    Source: ${fileName}, pg.${page}
                `;
                container.appendChild(badge);
            }
        });
        
        messageDiv.querySelector('.msg-content').appendChild(container);
        chatWindow.scrollTop = chatWindow.scrollHeight;
    };

    sendBtn.addEventListener("click", handleSend);
    queryInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") handleSend();
    });
});
