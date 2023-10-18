# Use the official Nginx image as the base image
FROM nginx:latest

# Copy your custom Nginx configuration file to the container
COPY nginx.conf /etc/nginx/nginx.conf

# (Optional) Copy your web application files to the container
# COPY /path/to/your/web/app /usr/share/nginx/html

# Expose port 80 for web traffic
EXPOSE 80

# Start Nginx when the container runs
CMD ["nginx", "-g", "daemon off;"]
