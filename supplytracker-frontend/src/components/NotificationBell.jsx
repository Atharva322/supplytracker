import { useState, useEffect } from 'react';
import '../styles/NotificationBell.css';

const NotificationBell = ({
  unreadCount,
  notifications,
  onMarkAsRead,
  onMarkAllAsRead,
  onDeleteNotification,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [displayedNotifications, setDisplayedNotifications] = useState([]);

  useEffect(() => {
    setDisplayedNotifications(notifications.slice(0, 10));
  }, [notifications]);

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'PRODUCT_CREATED':
        return '➕';
      case 'PRODUCT_UPDATED':
        return '✏️';
      case 'PRODUCT_DELETED':
        return '🗑️';
      case 'TRACKING_STAGE_ADDED':
        return '📍';
      default:
        return '📬';
    }
  };

  const getNotificationColor = (type) => {
    switch (type) {
      case 'PRODUCT_CREATED':
        return 'bg-green-50 border-green-200';
      case 'PRODUCT_UPDATED':
        return 'bg-blue-50 border-blue-200';
      case 'PRODUCT_DELETED':
        return 'bg-red-50 border-red-200';
      case 'TRACKING_STAGE_ADDED':
        return 'bg-yellow-50 border-yellow-200';
      default:
        return 'bg-gray-50 border-gray-200';
    }
  };

  const formatTime = (createdAt) => {
    const date = new Date(createdAt);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="notification-container">
      {/* Notification Bell */}
      <button
        className="notification-bell"
        onClick={() => setIsOpen(!isOpen)}
        title="Notifications"
      >
        🔔
        {unreadCount > 0 && (
          <span className="notification-badge">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Dropdown */}
      {isOpen && (
        <div className="notification-dropdown">
          {/* Header */}
          <div className="notification-header">
            <h3>Notifications</h3>
            {unreadCount > 0 && (
              <button
                className="mark-all-read-btn"
                onClick={() => {
                  onMarkAllAsRead();
                  setIsOpen(false);
                }}
                title="Mark all as read"
              >
                ✓ Mark all
              </button>
            )}
          </div>

          {/* Notifications List */}
          <div className="notification-list">
            {displayedNotifications.length === 0 ? (
              <div className="empty-notifications">
                <p>📭 No notifications</p>
              </div>
            ) : (
              displayedNotifications.map((notification) => (
                <div
                  key={notification.id}
                  className={`notification-item ${
                    notification.read ? 'read' : 'unread'
                  } ${getNotificationColor(notification.type)}`}
                  onClick={() => {
                    if (!notification.read) {
                      onMarkAsRead(notification.id);
                    }
                  }}
                >
                  <div className="notification-icon">
                    {getNotificationIcon(notification.type)}
                  </div>
                  <div className="notification-content">
                    <p className="notification-title">{notification.title}</p>
                    <p className="notification-message">{notification.message}</p>
                    <small className="notification-time">
                      {formatTime(notification.createdAt)}
                    </small>
                  </div>
                  <button
                    className="delete-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      onDeleteNotification(notification.id);
                    }}
                    title="Delete"
                  >
                    ✕
                  </button>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          {notifications.length > 10 && (
            <div className="notification-footer">
              <small>{notifications.length} total notifications</small>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationBell;