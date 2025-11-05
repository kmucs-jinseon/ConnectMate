import React, { useState } from 'react';
import './Chat.css';

const Chat = () => {
  const [messages, setMessages] = useState([
    {
      id: 1,
      sender: 'John',
      text: 'Hey everyone! Ready for soccer today?',
      time: '10:30 AM',
      isMine: false
    },
    {
      id: 2,
      sender: 'You',
      text: 'Yes! Can\'t wait!',
      time: '10:32 AM',
      isMine: true
    },
    {
      id: 3,
      sender: 'Sarah',
      text: 'I\'ll bring extra water bottles',
      time: '10:35 AM',
      isMine: false
    },
    {
      id: 4,
      sender: 'Mike',
      text: 'Thanks! See you at 3 PM',
      time: '10:40 AM',
      isMine: false
    }
  ]);

  const [newMessage, setNewMessage] = useState('');

  const chatRooms = [
    {
      id: 1,
      name: 'Weekly Soccer Match',
      lastMessage: 'Thanks! See you at 3 PM',
      time: '10:40 AM',
      unread: 0,
      icon: '⚽'
    },
    {
      id: 2,
      name: 'Study Group - Java',
      lastMessage: 'Don\'t forget to bring your laptops',
      time: 'Yesterday',
      unread: 3,
      icon: '📚'
    },
    {
      id: 3,
      name: 'Coffee Meetup',
      lastMessage: 'The cafe looks nice!',
      time: 'Yesterday',
      unread: 1,
      icon: '☕'
    }
  ];

  const [selectedChat, setSelectedChat] = useState(chatRooms[0]);

  const handleSendMessage = () => {
    if (newMessage.trim()) {
      const now = new Date();
      const newMsg = {
        id: messages.length + 1,
        sender: 'You',
        text: newMessage,
        time: now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
        isMine: true
      };
      setMessages([...messages, newMsg]);
      setNewMessage('');
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="chat-container">
      <div className="chat-list">
        <div className="chat-list-header">
          <h2>Chats</h2>
        </div>
        {chatRooms.map((room) => (
          <div
            key={room.id}
            className={`chat-room-item ${selectedChat.id === room.id ? 'active' : ''}`}
            onClick={() => setSelectedChat(room)}
          >
            <div className="chat-room-icon">{room.icon}</div>
            <div className="chat-room-info">
              <div className="chat-room-name">{room.name}</div>
              <div className="chat-room-last-message">{room.lastMessage}</div>
            </div>
            <div className="chat-room-meta">
              <div className="chat-room-time">{room.time}</div>
              {room.unread > 0 && (
                <div className="chat-room-unread">{room.unread}</div>
              )}
            </div>
          </div>
        ))}
      </div>

      <div className="chat-messages">
        <div className="chat-header">
          <div className="chat-header-icon">{selectedChat.icon}</div>
          <div className="chat-header-info">
            <h3>{selectedChat.name}</h3>
            <p>4 members</p>
          </div>
        </div>

        <div className="messages-container">
          {messages.map((message) => (
            <div
              key={message.id}
              className={`message ${message.isMine ? 'message-mine' : 'message-other'}`}
            >
              {!message.isMine && (
                <div className="message-sender">{message.sender}</div>
              )}
              <div className="message-bubble">
                <p className="message-text">{message.text}</p>
                <span className="message-time">{message.time}</span>
              </div>
            </div>
          ))}
        </div>

        <div className="chat-input-container">
          <input
            type="text"
            className="chat-input"
            placeholder="Type a message..."
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyPress={handleKeyPress}
          />
          <button className="send-button" onClick={handleSendMessage}>
            ➤
          </button>
        </div>
      </div>
    </div>
  );
};

export default Chat;
