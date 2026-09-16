FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# Hugging Face Spaces runs containers as uid 1000.
RUN useradd -m -u 1000 user
USER user
ENV HOME=/home/user \
    PATH=/home/user/.local/bin:$PATH

WORKDIR /app

COPY --chown=user requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

COPY --chown=user . .

EXPOSE 7860
# Shell form so $PORT expands: Render (and most PaaS) inject the port to bind,
# while Hugging Face Spaces expects 7860 - the fallback keeps both working.
CMD uvicorn app:app --host 0.0.0.0 --port ${PORT:-7860}
