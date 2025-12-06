import React, { useEffect, useRef, useState } from 'react';
import './MapView.css';

const MapView = () => {
  const mapContainer = useRef(null);
  const mapInstanceRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [selectedActivity, setSelectedActivity] = useState(null);
  const [mapError, setMapError] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredActivities, setFilteredActivities] = useState([]);
  const [userLocation, setUserLocation] = useState(null);

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

  // Initialize filtered activities
  useEffect(() => {
    setFilteredActivities(activities);
  }, []);

  // Get user's current location
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          setUserLocation({ lat: latitude, lng: longitude });
          console.log('User location obtained:', latitude, longitude);
        },
        (error) => {
          console.error('Geolocation error:', error);
          // Fallback to Seoul if geolocation fails
          setUserLocation({ lat: 37.5665, lng: 126.9780 });
        }
      );
    } else {
      console.log('Geolocation not supported, using default location');
      setUserLocation({ lat: 37.5665, lng: 126.9780 });
    }
  }, []);

  // Handle search functionality
  const handleSearch = (query) => {
    setSearchQuery(query);

    if (!query.trim()) {
      setFilteredActivities(activities);
      return;
    }

    const searchLower = query.toLowerCase();
    const filtered = activities.filter((activity) => {
      return (
        activity.title.toLowerCase().includes(searchLower) ||
        activity.location.toLowerCase().includes(searchLower) ||
        activity.description.toLowerCase().includes(searchLower) ||
        activity.category.toLowerCase().includes(searchLower)
      );
    });

    setFilteredActivities(filtered);
  };

  useEffect(() => {
    // Wait for user location before initializing map
    if (!userLocation) return;

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
              center: new window.kakao.maps.LatLng(userLocation.lat, userLocation.lng),
              level: 7
            };

            const kakaoMap = new window.kakao.maps.Map(container, options);
            mapInstanceRef.current = kakaoMap;

            // Add user location marker
            const userMarkerPosition = new window.kakao.maps.LatLng(userLocation.lat, userLocation.lng);
            const userMarker = new window.kakao.maps.Marker({
              position: userMarkerPosition,
              map: kakaoMap,
              title: 'Your Location'
            });

            const userInfoWindow = new window.kakao.maps.InfoWindow({
              content: '<div style="padding:10px;font-size:12px;">📍 Your Location</div>'
            });
            userInfoWindow.open(kakaoMap, userMarker);

            // Add markers for activities
            // Note: In Kakao Maps, we show all activities. Filtering is available in the fallback map.
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
  }, [userLocation]);

  const handleMarkerClick = (activity) => {
    setSelectedActivity(activity);
  };

  const handleCurrentLocation = () => {
    if (userLocation && mapInstanceRef.current) {
      const moveLatLng = new window.kakao.maps.LatLng(userLocation.lat, userLocation.lng);
      mapInstanceRef.current.setCenter(moveLatLng);
    }
  };

  return (
    <div className="map-view">
      {/* Search Bar */}
      <div className="search-bar">
        <input
          type="text"
          placeholder="Search activities or places..."
          className="search-input"
          value={searchQuery}
          onChange={(e) => handleSearch(e.target.value)}
          onKeyPress={(e) => {
            if (e.key === 'Enter') {
              handleSearch(searchQuery);
            }
          }}
        />
        <button
          className="search-button"
          onClick={() => handleSearch(searchQuery)}
        >
          🔍
        </button>
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
            {filteredActivities.map((activity, index) => (
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
            {filteredActivities.length > 0 ? (
              filteredActivities.map((activity) => (
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
              ))
            ) : (
              <div className="no-results">
                <p>No activities found matching "{searchQuery}"</p>
              </div>
            )}
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
        <button className="control-btn" title="Current Location" onClick={handleCurrentLocation}>
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
