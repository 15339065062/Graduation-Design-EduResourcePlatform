const axios = require('axios');

const token = 'eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI4IiwidXNlcm5hbWUiOiJtYWNoYW8iLCJyb2xlIjoidGVhY2hlciIsImlhdCI6MTc2ODM4NDEwNywiZXhwIjoxNzY4OTg4OTA3fQ.9HnrRN8WefPzmZHlAhyHm4OlFzUOSbtE1uT539rtBtX763BGo7RtJs62qJLOm5Pr';

axios.post('http://localhost:8080/api/comment', {
  resourceId: 7,
  content: 'Test comment from node script'
}, {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
.then(response => {
  console.log('Success:', response.data);
})
.catch(error => {
  if (error.response) {
    console.error('Error:', error.response.status, error.response.data);
  } else {
    console.error('Error:', error.message);
  }
});
