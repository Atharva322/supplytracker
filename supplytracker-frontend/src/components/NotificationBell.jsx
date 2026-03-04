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
    console.log('Fetched notifications:', notifications);
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

  return (
    <>
      {/* Notification Bell - Fixed on Right Side */}
      <button
        className="fixed right-6 top-20 z-40 notification-bell-fixed"
        onClick={() => setIsOpen(!isOpen)}
        title="Notifications"
      >
        🔔
        {unreadCount > 0 && (
          <span className="notification-badge-fixed">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Panel - Right Side Popup */}
      {isOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-30 bg-black/30"
            onClick={() => setIsOpen(false)}
          />

          {/* Right Side Panel */}
          <div className="notification-modal-center">
          {/* Header */}
          <div className="notification-panel-header">
            <h3>Notifications</h3>
            <button
              onClick={() => setIsOpen(false)}
              className="text-2xl leading-none text-gray-500 hover:text-gray-700"
            >
              ✕
            </button>
          </div>

            {/* Mark All As Read Button */}
            {unreadCount > 0 && (
              <div className="px-4 py-2 border-b border-gray-200 bg-blue-50">
                <button
                  className="text-sm text-blue-600 hover:text-blue-800 font-semibold"
                  onClick={() => {
                    onMarkAllAsRead();
                    setIsOpen(false);
                  }}
                  title="Mark all as read"
                >
                  ✓ Mark all as read
                </button>
              </div>
            )}

            {/* Notifications List */}
            <div className="notification-panel-list">
              {displayedNotifications.length === 0 ? (
                <div className="empty-notifications-panel">
                  <p>📭 No notifications</p>
                </div>
              ) : (
                displayedNotifications.map((notification) => (
                  <div
                    key={notification.id}
                    className={`notification-panel-item ${
                      notification.read ? 'read' : 'unread'
                    } ${getNotificationColor(notification.type)}`}
                    onClick={() => {
                      if (!notification.read) {
                        onMarkAsRead(notification.id);
                      }
                    }}
                  >
                    <div className="notification-panel-icon">
                      {getNotificationIcon(notification.type)}
                    </div>
                    <div className="notification-panel-content">
                      <p className="notification-panel-title" style={{ color: 'black' }}>
                        {notification.title}
                      </p>
                      <p className="notification-panel-message" style={{ color: 'black' }}>
                        {notification.message}
                      </p>
                    </div>
                    <button
                      className="notification-panel-delete-btn"
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
              <div className="notification-panel-footer">
                <small>{notifications.length} total notifications</small>
              </div>
            )}
          </div>
        </>
      )}
    </>
  );
};

export default NotificationBell;