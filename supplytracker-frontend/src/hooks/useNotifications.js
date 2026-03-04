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

  console.log('🔔 useNotifications rendered with:', { userId, hasToken: !!token });

  // Fetch notifications from REST API
  const fetchNotifications = useCallback(async () => {
    if (!token) {
      console.warn('⚠️ fetchNotifications: No token available');
      return;
    }

    console.log('📥 Fetching notifications...');
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
        console.log('✅ Notifications fetched:', data);
        setNotifications(Array.isArray(data) ? data : []);

        // Calculate unread count
        const notifArray = Array.isArray(data) ? data : [];
        const unread = notifArray.filter((n) => !n.read).length;
        setUnreadCount(unread);
        console.log('📊 Unread count:', unread);
      } else {
        console.error('❌ Failed to fetch notifications, status:', response.status);
      }
    } catch (err) {
      console.error('❌ Error fetching notifications:', err);
      setError(err.message);
      setNotifications([]);
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
        console.log('📊 Unread count response:', data);
        setUnreadCount(data.unreadCount || 0);
      }
    } catch (err) {
      console.error('Error fetching unread count:', err);
    }
  }, [token]);

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

  // Connect to WebSocket
  const connectWebSocket = useCallback(() => {
    console.log('🔌 connectWebSocket called with:', { userId, hasToken: !!token, isConnected });
    
    if (isConnected || !userId || !token) {
      console.warn('⚠️ Skipping WebSocket connection:', { userId, token: !!token, isConnected });
      return;
    }

    try {
      console.log('🚀 Creating WebSocket connection...');
      
      if (!window.SockJS || !window.Stomp) {
        console.error('❌ SockJS or Stomp not loaded on window');
        return;
      }

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
            console.log('📬 New notification via WebSocket:', notification);
            
            setNotifications((prev) => [notification, ...prev]);
            setUnreadCount((prev) => prev + 1);

            showBrowserNotification(notification);
          });

          console.log('✅ Subscribed to notifications queue');
        },
        (error) => {
          console.error('❌ WebSocket connection error:', error);
          isConnected = false;
          setError('Connection error. Notifications will sync periodically.');
          
          // Retry connection after 5 seconds
          setTimeout(() => connectWebSocket(), 5000);
        }
      );
    } catch (err) {
      console.error('❌ Error setting up WebSocket:', err);
      setError(err.message);
      isConnected = false;
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

  // Initialize WebSocket connection and fetch initial data
  useEffect(() => {
    console.log('📌 useEffect triggered with userId and token:', { userId, hasToken: !!token });
    
    if (userId && token) {
      console.log('✅ Both userId and token available, initializing...');
      connectWebSocket();
      fetchNotifications();
      requestNotificationPermission();

      // Fetch unread count every 30 seconds as fallback
      const interval = setInterval(fetchUnreadCount, 30000);

      return () => {
        console.log('🧹 Cleaning up notifications...');
        clearInterval(interval);
        disconnectWebSocket();
      };
    } else {
      console.warn('⚠️ useEffect: Missing userId or token', { userId, token });
      setNotifications([]);
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