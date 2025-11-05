import React, { useState } from 'react';
import './App.css';
import MapView from './components/MapView';
import ActivityList from './components/ActivityList';
import Chat from './components/Chat';
import Settings from './components/Settings';

function App() {
  const [activeTab, setActiveTab] = useState('map');

  const renderContent = () => {
    switch (activeTab) {
      case 'map':
        return <MapView />;
      case 'activity':
        return <ActivityList />;
      case 'chat':
        return <Chat />;
      case 'settings':
        return <Settings />;
      default:
        return <MapView />;
    }
  };

  return (
    <div className="App">
      <div className="content-container">
        {renderContent()}
      </div>

      <nav className="bottom-nav">
        <button
          className={`nav-item ${activeTab === 'map' ? 'active' : ''}`}
          onClick={() => setActiveTab('map')}
        >
          <span className="nav-icon">🗺️</span>
          <span className="nav-label">Map</span>
        </button>
        <button
          className={`nav-item ${activeTab === 'activity' ? 'active' : ''}`}
          onClick={() => setActiveTab('activity')}
        >
          <span className="nav-icon">📅</span>
          <span className="nav-label">Activity</span>
        </button>
        <button
          className={`nav-item ${activeTab === 'chat' ? 'active' : ''}`}
          onClick={() => setActiveTab('chat')}
        >
          <span className="nav-icon">💬</span>
          <span className="nav-label">Chat</span>
        </button>
        <button
          className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          <span className="nav-icon">⚙️</span>
          <span className="nav-label">Settings</span>
        </button>
      </nav>
    </div>
  );
}

export default App;
