# ELK (Elasticsearch + Logstash + Kibana)

Luồng: service Java --(TCP JSON, cổng 5000)--> Logstash --> Elasticsearch (index music4-logs-YYYY.MM.dd) --> Kibana.

- Kibana: http://localhost:5601  (data view "Music4 logs" tạo sẵn bởi container kibana-setup)
- Discover, chọn "Music4 logs", gõ KQL, ví dụ:
    service : "user-service"
    level : "ERROR"
    message : *Flyway*
    service : "api-gateway" and level : "WARN"
- Trường có sẵn: @timestamp, service, level, logger_name, message, stack_trace, thread_name, traceId, spanId (khi có tracing).
