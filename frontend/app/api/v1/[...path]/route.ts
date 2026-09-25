import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

function getBackendBaseUrl(): string {
  let backend = (process.env.INTERNAL_BACKEND_URL || process.env.BACKEND_URL || 'http://smartdesk-backend:8080').trim();

  // If already contains a domain like onrender.com
  if (backend.includes('.onrender.com')) {
    return backend.startsWith('http') ? backend : `https://${backend}`;
  }

  // Extract hostname without protocol and port
  const clean = backend.replace(/^https?:\/\//, '');
  const [hostWithoutPort] = clean.split(':');
  const [hostname] = hostWithoutPort.split('/');

  // On Render Free Tier, Web Services do not support private networking inbound (causing getaddrinfo ENOTFOUND).
  // If the hostname is a Render service identifier (e.g. smartdesk-backend-hjhm) or running on Render,
  // rewrite to the public HTTPS URL: https://<hostname>.onrender.com
  const isRenderEnvironment = Boolean(process.env.RENDER || process.env.RENDER_SERVICE_ID || process.env.RENDER_INSTANCE_ID);
  const isRenderSlug = /^[a-zA-Z0-9]+(-[a-zA-Z0-9]+)+$/.test(hostname) && !hostname.includes('.') && hostname !== 'localhost';

  if ((isRenderEnvironment || isRenderSlug) && (hostname.startsWith('smartdesk-backend-') || hostname === 'smartdesk-backend')) {
    if (hostname.startsWith('smartdesk-backend-')) {
      return `https://${hostname}.onrender.com`;
    }
  }

  if (!backend.startsWith('http://') && !backend.startsWith('https://')) {
    backend = `http://${backend}`;
  }
  return backend;
}

async function proxyRequest(request: NextRequest, params: { path: string[] }) {
  const backendBase = getBackendBaseUrl();
  const subPath = params.path ? params.path.join('/') : '';
  const search = request.nextUrl.search;
  let targetUrl = `${backendBase}/api/v1/${subPath}${search}`;

  try {
    const headers = new Headers();
    request.headers.forEach((value, key) => {
      if (key.toLowerCase() !== 'host') {
        headers.set(key, value);
      }
    });

    const body = ['GET', 'HEAD'].includes(request.method) ? undefined : await request.arrayBuffer();

    let backendResponse: Response;
    try {
      backendResponse = await fetch(targetUrl, {
        method: request.method,
        headers,
        body,
      });
    } catch (fetchError) {
      // If DNS resolution failed (getaddrinfo ENOTFOUND) and target was an internal Render hostname
      const errStr = String(fetchError);
      const hostMatch = targetUrl.match(/^https?:\/\/([^/:]+)/);
      const host = hostMatch ? hostMatch[1] : '';
      if (errStr.includes('ENOTFOUND') && host && !host.includes('.') && host !== 'localhost') {
        const fallbackUrl = targetUrl.replace(/^https?:\/\/[^/:]+(?::\d+)?/, `https://${host}.onrender.com`);
        targetUrl = fallbackUrl;
        backendResponse = await fetch(fallbackUrl, {
          method: request.method,
          headers,
          body,
        });
      } else {
        throw fetchError;
      }
    }

    const responseHeaders = new Headers();
    backendResponse.headers.forEach((value, key) => {
      if (!['transfer-encoding', 'content-encoding'].includes(key.toLowerCase())) {
        responseHeaders.set(key, value);
      }
    });

    const responseBody = await backendResponse.arrayBuffer();

    return new NextResponse(responseBody, {
      status: backendResponse.status,
      statusText: backendResponse.statusText,
      headers: responseHeaders,
    });
  } catch (error) {
    const cause = (error as { cause?: unknown })?.cause;
    const causeStr = cause ? ` (${cause instanceof Error ? cause.message : JSON.stringify(cause)})` : '';
    const errMessage = error instanceof Error ? `${error.message}${causeStr}` : 'Unknown proxy error';
    return NextResponse.json(
      {
        success: false,
        error: 'PROXY_GATEWAY_ERROR',
        message: `Failed to communicate with SmartDesk backend at ${targetUrl}: ${errMessage}`,
      },
      { status: 502 }
    );
  }
}

export async function GET(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, await params);
}

export async function POST(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, await params);
}

export async function PUT(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, await params);
}

export async function PATCH(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, await params);
}

export async function DELETE(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, await params);
}

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 204,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, Accept, X-Requested-With',
    },
  });
}
