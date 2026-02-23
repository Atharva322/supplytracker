import { useEffect, useState, useCallback } from 'react';

let stompClient = null;
let isConnected = false;

const API_BASE_URL = import.meta.env.VITE_API_URL 
  ? `${import.meta.env.VITE_API_URL}/api`
  : 'http://localhost:8080/api';

const useNotifications = (userId, token) => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // Connect to WebSocket
  const connectWebSocket = useCallback(() => {
    if (isConnected || !userId || !token) return;

    try {
      const socket = new window.SockJS(`${API_BASE_URL.replace('/api', '')}/ws/notifications`);
      stompClient = window.Stomp.over(socket);

      stompClient.connect(
        { username: userId, token: token },
        (frame) => {
          console.log('✅ Connected to WebSocket:', frame);
          isConnected = true;

          // Subscribe to personal notification queue
          stompClient.subscribe(`/user/${userId}/queue/notifications`, (msg) => {
            const notification = JSON.parse(msg.body);
            console.log('📬 New notification:', notification);
            
            setNotifications((prev) => [notification, ...prev]);
            setUnreadCount((prev) => prev + 1);

            showBrowserNotification(notification);
          });

          fetchNotifications();
        },
        (error) => {
          console.error('❌ WebSocket connection error:', error);
          isConnected = false;
          setError('Connection error. Notifications will sync periodically.');
          
          // Retry connection after 5 seconds
          setTimeout(connectWebSocket, 5000);
        }
      );
    } catch (err) {
      console.error('Error setting up WebSocket:', err);
      setError(err.message);
    }
  }, [userId, token]);

  // Disconnect from WebSocket
  const disconnectWebSocket = useCallback(() => {
    if (stompClient && isConnected) {
      stompClient.disconnect(() => {
        console.log('❌ Disconnected from WebSocket');
        isConnected = false;
      });
    }
  }, []);

  // Fetch notifications from REST API
  const fetchNotifications = useCallback(async () => {
    if (!token) return;

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/notifications`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();
        setNotifications(data);

        // Calculate unread count
        const unread = data.filter((n) => !n.read).length;
        setUnreadCount(unread);
      }
    } catch (err) {
      console.error('Error fetching notifications:', err);
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  // Fetch unread count
  const fetchUnreadCount = useCallback(async () => {
    if (!token) return;

    try {
      const response = await fetch(`${API_BASE_URL}/notifications/unread-count`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();
        setUnreadCount(data.unreadCount);
      }
    } catch (err) {
      console.error('Error fetching unread count:', err);
    }
  }, [token]);

  // Mark notification as read
  const markAsRead = useCallback(
    async (notificationId) => {
      try {
        const response = await fetch(`${API_BASE_URL}/notifications/${notificationId}/read`, {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        });

        if (response.ok) {
          setNotifications((prev) =>
            prev.map((n) => (n.id === notificationId ? { ...n, read: true } : n))
          );
          setUnreadCount((prev) => Math.max(0, prev - 1));
        }
      } catch (err) {
        console.error('Error marking notification as read:', err);
      }
    },
    [token]
  );

  // Mark all notifications as read
  const markAllAsRead = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/notifications/read-all`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
        setUnreadCount(0);
      }
    } catch (err) {
      console.error('Error marking all as read:', err);
    }
  }, [token]);

  // Delete notification
  const deleteNotification = useCallback(
    async (notificationId) => {
      try {
        const response = await fetch(`${API_BASE_URL}/notifications/${notificationId}`, {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        });

        if (response.ok) {
          setNotifications((prev) => prev.filter((n) => n.id !== notificationId));
        }
      } catch (err) {
        console.error('Error deleting notification:', err);
      }
    },
    [token]
  );

  // Show browser notification
  const showBrowserNotification = (notification) => {
    if ('Notification' in window && window.Notification.permission === 'granted') {
      new window.Notification(notification.title, {
        body: notification.message,
        icon: '🔔',
        tag: notification.id,
      });
    }
  };

  // Request browser notification permission
  const requestNotificationPermission = useCallback(() => {
    if ('Notification' in window && window.Notification.permission === 'default') {
      window.Notification.requestPermission();
    }
  }, []);

  // Initialize WebSocket connection and fetch initial data
  useEffect(() => {
    if (userId && token) {
      connectWebSocket();
      fetchNotifications();
      requestNotificationPermission();

      // Fetch unread count every 30 seconds as fallback
      const interval = setInterval(fetchUnreadCount, 30000);

      return () => {
        clearInterval(interval);
        disconnectWebSocket();
      };
    }
  }, [userId, token, connectWebSocket, disconnectWebSocket, fetchNotifications, fetchUnreadCount, requestNotificationPermission]);

  return {
    notifications,
    unreadCount,
    isLoading,
    error,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    fetchNotifications,
  };
};

export default useNotifications;