FROM python:3.12-slim

WORKDIR /app

RUN pip install --no-cache-dir \
    azure-identity==1.25.3 \
    azure-keyvault-secrets==4.9.0 \
    azure-storage-blob==12.30.0 \
    "psycopg[binary]==3.3.4" \
    python-dotenv==1.2.2 \
    streamlit==1.61.1

COPY app.py .

EXPOSE 8501

CMD ["streamlit", "run", "app.py", "--server.address=0.0.0.0", "--server.port=8501", "--server.headless=true", "--browser.gatherUsageStats=false"]
