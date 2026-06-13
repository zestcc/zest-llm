{{/*
Admin 容器环境变量：JWT、SSO（多副本须配 Redis PKCE / 会话吊销）。
SSO 启用时 SPRING_DATA_REDIS_* 必须可达（deployment-admin 已注入）。
*/}}
{{- define "zest-llm.admin.jwtEnv" -}}
{{- if .Values.admin.jwt.existingSecret }}
            - name: ZEST_LLM_ADMIN_JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: {{ .Values.admin.jwt.existingSecret | quote }}
                  key: {{ .Values.admin.jwt.secretKey | default "secret" | quote }}
{{- else if .Values.admin.jwt.secret }}
            - name: ZEST_LLM_ADMIN_JWT_SECRET
              value: {{ .Values.admin.jwt.secret | quote }}
{{- end }}
{{- end }}

{{- define "zest-llm.admin.ssoEnv" -}}
{{- if .Values.admin.sso.enabled }}
            - name: ZEST_LLM_ADMIN_SSO_ENABLED
              value: "true"
            - name: ZEST_LLM_ADMIN_SSO_PROVIDER
              value: {{ .Values.admin.sso.provider | default "zest-sso" | quote }}
            - name: ZEST_LLM_ADMIN_SSO_ISSUER
              value: {{ .Values.admin.sso.issuer | quote }}
            - name: ZEST_LLM_ADMIN_SSO_DISCOVERY_URI
              value: {{ .Values.admin.sso.discoveryUri | quote }}
            - name: ZEST_LLM_ADMIN_SSO_CLIENT_ID
              value: {{ .Values.admin.sso.clientId | quote }}
{{- if .Values.admin.sso.existingSecret }}
            - name: ZEST_LLM_ADMIN_SSO_CLIENT_SECRET
              valueFrom:
                secretKeyRef:
                  name: {{ .Values.admin.sso.existingSecret | quote }}
                  key: {{ .Values.admin.sso.secretClientSecretKey | default "client-secret" | quote }}
{{- else if .Values.admin.sso.clientSecret }}
            - name: ZEST_LLM_ADMIN_SSO_CLIENT_SECRET
              value: {{ .Values.admin.sso.clientSecret | quote }}
{{- end }}
            - name: ZEST_LLM_ADMIN_SSO_REDIRECT_URI
              value: {{ .Values.admin.sso.redirectUri | quote }}
            - name: ZEST_LLM_ADMIN_SSO_POST_LOGOUT_REDIRECT_URI
              value: {{ .Values.admin.sso.postLogoutRedirectUri | quote }}
{{- end }}
{{- end }}
