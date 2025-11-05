import React, { useState } from 'react';
import './Settings.css';

const Settings = () => {
  const [user] = useState({
    name: 'Demo User',
    username: '@demouser',
    email: 'demo@connectmate.com',
    avatar: '👤'
  });

  const menuItems = [
    { id: 1, icon: '👤', title: 'Account', description: 'Manage your account settings' },
    { id: 2, icon: '🔔', title: 'Notifications', description: 'Configure notification preferences' },
    { id: 3, icon: '🔒', title: 'Privacy', description: 'Control your privacy settings' },
    { id: 4, icon: '🌐', title: 'Language', description: 'English' },
    { id: 5, icon: '🌙', title: 'Dark Mode', description: 'Enable dark mode', toggle: true },
    { id: 6, icon: '❓', title: 'Help & Support', description: 'Get help or contact support' },
    { id: 7, icon: 'ℹ️', title: 'About', description: 'App version 1.0.0' }
  ];

  return (
    <div className="settings-container">
      {/* Profile Section */}
      <div className="profile-section">
        <div className="profile-avatar">{user.avatar}</div>
        <div className="profile-info">
          <h2 className="profile-name">{user.name}</h2>
          <p className="profile-username">{user.username}</p>
          <p className="profile-email">{user.email}</p>
        </div>
        <button className="edit-profile-btn">Edit Profile</button>
      </div>

      {/* Settings Menu */}
      <div className="settings-menu">
        <h3 className="settings-section-title">Settings</h3>
        {menuItems.map((item) => (
          <div key={item.id} className="settings-item">
            <div className="settings-item-icon">{item.icon}</div>
            <div className="settings-item-content">
              <h4 className="settings-item-title">{item.title}</h4>
              <p className="settings-item-description">{item.description}</p>
            </div>
            {item.toggle ? (
              <label className="toggle-switch">
                <input type="checkbox" />
                <span className="toggle-slider"></span>
              </label>
            ) : (
              <span className="settings-item-arrow">›</span>
            )}
          </div>
        ))}
      </div>

      {/* Account Actions */}
      <div className="account-actions">
        <button className="action-btn">
          <span className="action-icon">🔑</span>
          Change Password
        </button>
        <button className="action-btn logout-btn">
          <span className="action-icon">🚪</span>
          Logout
        </button>
      </div>

      {/* App Info */}
      <div className="app-info">
        <p>ConnectMate Web Simulator</p>
        <p>Version 1.0.0 (Demo)</p>
        <p>© 2025 ConnectMate. All rights reserved.</p>
      </div>
    </div>
  );
};

export default Settings;
