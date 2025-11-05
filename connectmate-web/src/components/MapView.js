import React, { useEffect, useRef, useState } from 'react';
import './MapView.css';

const MapView = () => {
  const mapContainer = useRef(null);
  const [loading, setLoading] = useState(true);
  const [selectedActivity, setSelectedActivity] = useState(null);
  const [mapError, setMapError] = useState(false);

  // Sample activity markers (same as Android app)
  const activities = [
    {
      id: '1',
      title: 'Weekly Soccer Match',
      location: 'Seoul National Park',
      time: 'Today, 3:00 PM',
      description: 'Join us for a friendly soccer match!',
      participants: 5,
      maxParticipants: 10,
      lat: 37.5665,
      lng: 126.9780,
      category: 'Sports',
      color: '#4CAF50'
    },
    {
      id: '2',
      title: 'Study Group - Java',
      location: 'Gangnam Library',
      time: 'Tomorrow, 2:00 PM',
      description: 'Let\'s study Java together',
      participants: 3,
      maxParticipants: 8,
      lat: 37.5700,
      lng: 126.9850,
      category: 'Study',
      color: '#2196F3'
    },
    {
      id: '3',
      title: 'Coffee Meetup',
      location: 'Hongdae Cafe',
      time: 'Saturday, 4:00 PM',
      description: 'Casual coffee and chat',
      participants: 6,
      maxParticipants: 12,
      lat: 37.5550,
      lng: 126.9200,
      category: 'Social',
      color: '#FF9800'
    }
  ];

  useEffect(() => {
    // Try to initialize Kakao Maps
    const script = document.createElement('script');
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=76e9f68c2c56d701f233ba2b44e74ea1&autoload=false`;
    script.async = true;
    document.head.appendChild(script);

    const timeout = setTimeout(() => {
      if (loading) {
        console.log('Kakao Maps failed to load, using fallback map');
        setMapError(true);
        setLoading(false);
      }
    }, 5000);

    script.onload = () => {
      if (window.kakao && window.kakao.maps) {
        window.kakao.maps.load(() => {
          try {
            const container = mapContainer.current;
            const options = {
              center: new window.kakao.maps.LatLng(37.5665, 126.9780),
              level: 7
            };

            const kakaoMap = new window.kakao.maps.Map(container, options);

            // Add markers for activities
            activities.forEach((activity) => {
              const markerPosition = new window.kakao.maps.LatLng(activity.lat, activity.lng);
              const marker = new window.kakao.maps.Marker({
                position: markerPosition,
                map: kakaoMap
              });

              const infowindow = new window.kakao.maps.InfoWindow({
                content: `<div style="padding:10px;font-size:12px;width:200px;">
                  <strong>${activity.title}</strong><br/>
                  ${activity.location}<br/>
                  ${activity.time}<br/>
                  <span style="color:#2196F3;">${activity.participants}/${activity.maxParticipants} participants</span>
                </div>`
              });

              window.kakao.maps.event.addListener(marker, 'click', () => {
                infowindow.open(kakaoMap, marker);
              });
            });

            setLoading(false);
            clearTimeout(timeout);
          } catch (error) {
            console.error('Kakao Maps error:', error);
            setMapError(true);
            setLoading(false);
            clearTimeout(timeout);
          }
        });
      } else {
        setMapError(true);
        setLoading(false);
        clearTimeout(timeout);
      }
    };

    script.onerror = () => {
      console.error('Failed to load Kakao Maps script');
      setMapError(true);
      setLoading(false);
      clearTimeout(timeout);
    };

    return () => {
      clearTimeout(timeout);
      script.remove();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleMarkerClick = (activity) => {
    setSelectedActivity(activity);
  };

  return (
    <div className="map-view">
      {/* Search Bar */}
      <div className="search-bar">
        <input
          type="text"
          placeholder="Search activities or places..."
          className="search-input"
        />
        <button className="search-button">🔍</button>
      </div>

      {/* Map Container */}
      {mapError ? (
        // Fallback Interactive Map
        <div className="fallback-map-container">
          <div className="fallback-map">
            <div className="map-overlay">
              <p className="map-title">Seoul, South Korea</p>
              <p className="map-subtitle">Interactive Activity Map</p>
            </div>

            {/* Activity Markers */}
            {activities.map((activity, index) => (
              <div
                key={activity.id}
                className="activity-marker"
                style={{
                  left: `${30 + index * 20}%`,
                  top: `${40 + (index % 2) * 15}%`,
                  backgroundColor: activity.color
                }}
                onClick={() => handleMarkerClick(activity)}
              >
                <div className="marker-pin">📍</div>
                <div className="marker-pulse"></div>
              </div>
            ))}
          </div>

          {/* Activity Info Cards */}
          <div className="activity-cards">
            {activities.map((activity) => (
              <div
                key={activity.id}
                className={`activity-info-card ${selectedActivity?.id === activity.id ? 'selected' : ''}`}
                onClick={() => handleMarkerClick(activity)}
              >
                <div className="card-header" style={{ backgroundColor: activity.color }}>
                  <span className="card-category">{activity.category}</span>
                </div>
                <div className="card-content">
                  <h4>{activity.title}</h4>
                  <p className="card-location">📍 {activity.location}</p>
                  <p className="card-time">🕒 {activity.time}</p>
                  <p className="card-participants">
                    👥 {activity.participants}/{activity.maxParticipants}
                  </p>
                </div>
              </div>
            ))}
          </div>

          {selectedActivity && (
            <div className="activity-detail-popup">
              <div className="popup-header">
                <h3>{selectedActivity.title}</h3>
                <button className="close-popup" onClick={() => setSelectedActivity(null)}>✕</button>
              </div>
              <div className="popup-content">
                <p><strong>Location:</strong> {selectedActivity.location}</p>
                <p><strong>Time:</strong> {selectedActivity.time}</p>
                <p><strong>Description:</strong> {selectedActivity.description}</p>
                <p><strong>Participants:</strong> {selectedActivity.participants}/{selectedActivity.maxParticipants}</p>
                <button className="join-btn-popup">Join Activity</button>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div ref={mapContainer} className="map-container">
          {loading && (
            <div className="loading-overlay">
              <div className="spinner"></div>
              <p>Loading Kakao Map...</p>
            </div>
          )}
        </div>
      )}

      {/* Map Controls */}
      <div className="map-controls">
        <button className="control-btn" title="Current Location">
          📍
        </button>
        <button className="control-btn" title="Zoom In">
          +
        </button>
        <button className="control-btn" title="Zoom Out">
          −
        </button>
      </div>
    </div>
  );
};

export default MapView;
