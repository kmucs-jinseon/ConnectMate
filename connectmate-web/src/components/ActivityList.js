import React from 'react';
import './ActivityList.css';

const ActivityList = () => {
  const activities = [
    {
      id: '1',
      title: 'Weekly Soccer Match',
      location: 'Seoul National Park',
      time: 'Today, 3:00 PM',
      description: 'Join us for a friendly soccer match!',
      participants: 5,
      maxParticipants: 10,
      category: 'Sports',
      image: '⚽'
    },
    {
      id: '2',
      title: 'Study Group - Java',
      location: 'Gangnam Library',
      time: 'Tomorrow, 2:00 PM',
      description: 'Let\'s study Java together',
      participants: 3,
      maxParticipants: 8,
      category: 'Study',
      image: '📚'
    },
    {
      id: '3',
      title: 'Coffee Meetup',
      location: 'Hongdae Cafe',
      time: 'Saturday, 4:00 PM',
      description: 'Casual coffee and chat',
      participants: 6,
      maxParticipants: 12,
      category: 'Social',
      image: '☕'
    },
    {
      id: '4',
      title: 'Hiking Adventure',
      location: 'Bukhansan',
      time: 'Sunday, 7:00 AM',
      description: 'Morning hike to enjoy nature',
      participants: 8,
      maxParticipants: 15,
      category: 'Sports',
      image: '🥾'
    },
    {
      id: '5',
      title: 'Movie Night',
      location: 'CGV Gangnam',
      time: 'Friday, 7:30 PM',
      description: 'Watch the latest blockbuster together',
      participants: 4,
      maxParticipants: 10,
      category: 'Entertainment',
      image: '🎬'
    }
  ];

  return (
    <div className="activity-list">
      <div className="activity-header">
        <h1>Activities</h1>
        <button className="create-activity-btn">+ Create New</button>
      </div>

      <div className="filter-tabs">
        <button className="filter-tab active">All</button>
        <button className="filter-tab">Sports</button>
        <button className="filter-tab">Study</button>
        <button className="filter-tab">Social</button>
      </div>

      <div className="activities-container">
        {activities.map((activity) => (
          <div key={activity.id} className="activity-card">
            <div className="activity-image">{activity.image}</div>
            <div className="activity-content">
              <div className="activity-badge">{activity.category}</div>
              <h3 className="activity-title">{activity.title}</h3>
              <p className="activity-location">📍 {activity.location}</p>
              <p className="activity-time">🕒 {activity.time}</p>
              <p className="activity-description">{activity.description}</p>
              <div className="activity-footer">
                <span className="participants">
                  👥 {activity.participants}/{activity.maxParticipants}
                </span>
                <button className="join-btn">Join</button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ActivityList;
