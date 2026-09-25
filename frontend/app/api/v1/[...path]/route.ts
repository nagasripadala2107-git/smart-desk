import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

function getBackendBaseUrl(): string {
  let backend = process.env.INTERNAL_BACKEND_URL || process.env.BACKEND_URL || 'http://smartdesk-backend:8080';
  if (!backend.startsWith('http://') && !backend.startsWith('https://')) {
    backend = `http://${backend}`;
  }
  return backend;
}

async function proxyRequest(request: NextRequest, params: { path: string[] }) {
  try {
    const backendBase = getBackendBaseUrl();
    const subPath = params.path ? params.path.join('/') : '';
    const search = request.nextUrl.search;
    const targetUrl = `${backendBase}/api/v1/${subPath}${search}`;

    const headers = new Headers();
    request.headers.forEach((value, key) => {
      if (key.toLowerCase() !== 'host') {
        headers.set(key, value);
      }
    });

    const body = ['GET', 'HEAD'].includes(request.method) ? undefined : await request.arrayBuffer();

    const backendResponse = await fetch(targetUrl, {
      method: request.method,
      headers,
      body,
    });

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
