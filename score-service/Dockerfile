FROM python.3.11

WORKDIR /app

# Copy shared components
COPY ../shared /app/shared

# Copy service-specific files
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8002

CMD ["python", "app.py"]