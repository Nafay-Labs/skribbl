'use strict';

// DOM Elements
const introScreen = document.getElementById('intro-screen');
const gameScreen = document.getElementById('game-screen');
const registerSection = document.getElementById('register-section');
const roomSection = document.getElementById('room-section');

const nicknameInput = document.getElementById('nickname-input');
const registerBtn = document.getElementById('register-btn');
const displayNickname = document.getElementById('display-nickname');
const createRoomBtn = document.getElementById('create-room-btn');
const roomIdInput = document.getElementById('room-id-input');
const joinRoomBtn = document.getElementById('join-room-btn');

const gameRoomIdDisplay = document.getElementById('game-room-id');
const chatMessages = document.getElementById('chat-messages');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');

const canvas = document.getElementById('drawing-board');
const ctx = canvas.getContext('2d');
const colorPicker = document.getElementById('color-picker');
const brushSize = document.getElementById('brush-size');

const lobbyOverlay = document.getElementById('lobby-overlay');
const lobbyPlayerList = document.getElementById('lobby-player-list');
const startGameBtn = document.getElementById('start-game-btn');

// State
let stompClient = null;
let playerId = null;
let nickname = null;
let currentRoomId = null;

let currentGameState = 'LOBBY';
let currentDrawerId = null;
let amIDrawing = false;

let isDrawing = false;
let lastX = 0;
let lastY = 0;

let lastEmittedX = 0;
let lastEmittedY = 0;
let lastEmitTime = 0;
const EMIT_THROTTLE_MS = 20;

// Connect to WebSocket after registration
async function registerPlayer() {
    const name = nicknameInput.value.trim();
    if (!name) return alert("Please enter a nickname");
    
    try {
        const response = await fetch(`/api/player/register?nickname=${encodeURIComponent(name)}`, { method: 'POST' });
        const data = await response.json();
        playerId = data.playerId;
        nickname = data.nickname;
        
        displayNickname.textContent = nickname;
        registerSection.classList.add('hidden');
        roomSection.classList.remove('hidden');
        
        connectWebSocket();
    } catch (err) {
        console.error("Error registering player:", err);
    }
}

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable excessive logging
    
    stompClient.connect({}, (frame) => {
        // Subscribe to user queue for private messages like room creation and initial strokes
        stompClient.subscribe('/user/queue/room/create', (msg) => {
            const response = JSON.parse(msg.body);
            if (response.content.includes("ID: ")) {
                const newRoomId = response.content.split("ID: ")[1];
                joinGameRoom(newRoomId);
            }
        });

        stompClient.subscribe('/user/queue/room/strokes', (msg) => {
            const strokes = JSON.parse(msg.body);
            strokes.forEach(stroke => drawLineOnCanvas(stroke.x1, stroke.y1, stroke.x2, stroke.y2, stroke.color, stroke.brushSize));
        });

        stompClient.subscribe('/user/queue/room/update', (msg) => {
            const response = JSON.parse(msg.body);
            updateRoomState(response.room);
        });
    }, (error) => {
        alert("Could not connect to WebSocket server. Please refresh.");
    });
}

function createRoom() {
    if (!stompClient || !playerId) return;
    stompClient.send("/app/room/create", {}, JSON.stringify(playerId));
}

function joinRoom() {
    const rId = roomIdInput.value.trim();
    if (!rId) return alert("Enter a Room ID");
    joinGameRoom(rId);
}

function joinGameRoom(roomId) {
    currentRoomId = roomId;
    
    // Subscribe to room topic
    stompClient.subscribe(`/topic/room/${roomId}`, onRoomMessageReceived);

    // Send join request
    stompClient.send(`/app/room/${roomId}/join`, {}, JSON.stringify(playerId));

    // Update UI
    gameRoomIdDisplay.textContent = roomId;
    introScreen.classList.add('hidden');
    gameScreen.classList.remove('hidden');
    
    // Resize canvas to fit properly after it becomes visible
    resizeCanvas();
}

function onRoomMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    if (message.type === 'DRAW') {
        drawLineOnCanvas(message.x1, message.y1, message.x2, message.y2, message.color, message.brushSize);
    } else if (message.type === 'BEGIN') {
        // Handled as draw line with same start/end points to make a dot
        drawLineOnCanvas(message.x1, message.y1, message.x2, message.y2, message.color, message.brushSize);
    } else if (message.type === 'CLEAR') {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    } else if (message.type === 'ROOM_UPDATE') {
        updateRoomState(message.room);
    } else if (message.type === 'JOIN_GAME' || message.type === 'LEAVE' || message.type === 'MESSAGE') {
        addChatMessage(message);
    }
}

function updateRoomState(room) {
    currentGameState = room.state;
    currentDrawerId = room.currentDrawer ? room.currentDrawer.playerId : null;
    amIDrawing = (currentDrawerId === playerId);

    // Update UI based on state
    if (currentGameState === 'LOBBY') {
        lobbyOverlay.classList.remove('hidden');
        document.getElementById('lobby-room-id-value').textContent = room.roomID;
        updateLobbyPlayers(room.players);
        
        // Only show start button for admin
        if (room.admin && room.admin.playerId === playerId) {
            startGameBtn.classList.remove('hidden');
        } else {
            startGameBtn.classList.add('hidden');
        }
    } else {
        lobbyOverlay.classList.add('hidden');
    }

    // Tools visibility
    const tools = document.querySelector('.tools-panel');
    if (amIDrawing) {
        tools.style.opacity = '1';
        tools.style.pointerEvents = 'all';
    } else {
        tools.style.opacity = '0.5';
        tools.style.pointerEvents = 'none';
    }
}

function updateLobbyPlayers(players) {
    lobbyPlayerList.innerHTML = '';
    players.forEach(p => {
        const li = document.createElement('li');
        li.textContent = p.nickname + (p.playerId === playerId ? ' (You)' : '');
        lobbyPlayerList.appendChild(li);
    });
}

function startGame() {
    if (!stompClient || !currentRoomId) return;
    stompClient.send(`/app/room/${currentRoomId}/start`, {}, {});
}

function addChatMessage(message) {
    const li = document.createElement('li');
    
    if (message.type === 'JOIN_GAME' || message.type === 'LEAVE') {
        li.className = 'msg-system';
        li.textContent = message.content;
    } else {
        li.innerHTML = `<span class="msg-user">${message.username}:</span> <span class="msg-content">${message.content}</span>`;
    }
    
    chatMessages.appendChild(li);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function sendChatMessage(e) {
    e.preventDefault();
    const content = chatInput.value.trim();
    // Assuming there's a chat send endpoint, but we don't have it defined in RoomController yet.
    // For now, let's pretend we have a basic message structure.
    // In a real scenario, you'd add @MessageMapping("/room/{roomId}/chat")
    if(content && stompClient) {
        // Just mock for now or wait for backend implementation
        chatInput.value = '';
    }
}

// Canvas Drawing Logic
function resizeCanvas() {
    const wrapper = canvas.parentElement;
    canvas.width = wrapper.clientWidth;
    canvas.height = wrapper.clientHeight;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
}

window.addEventListener('resize', () => {
    if (!gameScreen.classList.contains('hidden')) {
        // Note: Resizing clears the canvas. In a real app, you'd save image data and restore it.
        resizeCanvas();
    }
});

function getMousePos(evt) {
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    return {
        x: (evt.clientX - rect.left) * scaleX,
        y: (evt.clientY - rect.top) * scaleY
    };
}

function startDrawing(e) {
    if (!amIDrawing || currentGameState !== 'DRAWING') return;
    isDrawing = true;
    const pos = getMousePos(e);
    lastX = pos.x;
    lastY = pos.y;
    
    lastEmittedX = pos.x;
    lastEmittedY = pos.y;
    lastEmitTime = Date.now();
    
    emitDrawEvent('BEGIN', lastX, lastY, lastX, lastY);
}

function draw(e) {
    if (!isDrawing) return;
    
    const pos = getMousePos(e);
    const x = pos.x;
    const y = pos.y;
    
    // Draw locally immediately for smooth feeling
    drawLineOnCanvas(lastX, lastY, x, y, colorPicker.value, brushSize.value);
    
    // Throttle emitting to network
    const now = Date.now();
    if (now - lastEmitTime > EMIT_THROTTLE_MS) {
        emitDrawEvent('DRAW', lastEmittedX, lastEmittedY, x, y);
        lastEmittedX = x;
        lastEmittedY = y;
        lastEmitTime = now;
    }
    
    lastX = x;
    lastY = y;
}

function stopDrawing(e) {
    if (!isDrawing) return;
    isDrawing = false;
    
    // Emit any remaining stroke segment
    if (lastX !== lastEmittedX || lastY !== lastEmittedY) {
        emitDrawEvent('DRAW', lastEmittedX, lastEmittedY, lastX, lastY);
    }
}

function drawLineOnCanvas(x1, y1, x2, y2, color, size) {
    ctx.beginPath();
    ctx.strokeStyle = color;
    ctx.lineWidth = size;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();
    ctx.closePath();
}

function emitDrawEvent(type, x1, y1, x2, y2) {
    if (!stompClient || !currentRoomId) return;
    
    const event = {
        type: type,
        x1: Math.round(x1),
        y1: Math.round(y1),
        x2: Math.round(x2),
        y2: Math.round(y2),
        color: colorPicker.value,
        brushSize: parseInt(brushSize.value)
    };
    
    stompClient.send(`/app/room/${currentRoomId}/draw`, {}, JSON.stringify(event));
}

// Event Listeners
registerBtn.addEventListener('click', registerPlayer);
nicknameInput.addEventListener('keypress', (e) => e.key === 'Enter' && registerPlayer());

createRoomBtn.addEventListener('click', createRoom);
joinRoomBtn.addEventListener('click', joinRoom);
roomIdInput.addEventListener('keypress', (e) => e.key === 'Enter' && joinRoom());

chatForm.addEventListener('submit', sendChatMessage);

startGameBtn.addEventListener('click', startGame);

canvas.addEventListener('mousedown', startDrawing);
canvas.addEventListener('mousemove', draw);
canvas.addEventListener('mouseup', stopDrawing);
canvas.addEventListener('mouseout', stopDrawing);