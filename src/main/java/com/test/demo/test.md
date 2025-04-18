// routes/opal/opal-route.js
import { BaseRoute } from '../baseRoute.js';
import jwtDecode from 'jwt-decode';
import config from '../../config/environment/index.js';
import fetch from 'node-fetch';

export class OpalRoute extends BaseRoute {
  constructor(server, ssoService) {
    super(server);
    this.bindMethods([
      'postUiAccess',
      'postBffAccess',
    ]);
    this.createRoutes();
  }

  createRoutes() {
    // Both endpoints are now POST
    this.server.post(
      `${this.baseRoute}/opal/ui`,
      this.ensureAuthenticated,
      this.validateUiRequest,
      this.postUiAccess
    );

    this.server.post(
      `${this.baseRoute}/opal/bff`,
      this.ensureAuthenticated,
      this.validateBffRequest,
      this.postBffAccess
    );
  }

  // Validate that nothing extra is required for UI (just auth + roles)
  validateUiRequest(req, res, next) {
    // If in future you need body params, assert here.
    return next();
  }

  // Ensure componentType and action are present
  validateBffRequest(req, res, next) {
    req.assert('componentType', 'componentType is required').notEmpty();
    req.assert('action', 'action is required').notEmpty();

    const errors = req.validationErrors();
    if (errors) {
      return res.send(400, { errors });
    }
    return next();
  }

  // Helper to extract roles from the JWT saved in req.user.context
  getRolesFromContext(req) {
    try {
      const token = req.user.context;
      const { custom_group = [] } = jwtDecode(token);
      return Array.isArray(custom_group)
        ? custom_group.filter(g => typeof g === 'string')
        : [];
    } catch (err) {
      throw new Error('Invalid JWT context');
    }
  }

  async postUiAccess(req, res, next) {
    try {
      const roles = this.getRolesFromContext(req);
      const { host, port, uiPath } = config.opalClient;
      const url = `http://${host}:${port}${uiPath}`;

      // POST roles as JSON (rather than query string)
      const sidecarResp = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ roles }),
        agent: req._httpMessage.agent, // reuses your custom HTTPS agent if needed
      });

      const payload = await sidecarResp.json();
      return res.send(sidecarResp.status, payload);
    } catch (err) {
      req.log.error(err);
      return res.send(502, { error: 'Failed to fetch UI access', message: err.message });
    } finally {
      next();
    }
  }

  async postBffAccess(req, res, next) {
    try {
      const roles = this.getRolesFromContext(req);
      const { componentType, action } = req.body;
      const { host, port, bffPath } = config.opalClient;
      const url = `http://${host}:${port}${bffPath}`;

      const sidecarResp = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ roles, componentType, action }),
        agent: req._httpMessage.agent,
      });

      const resultBody = await sidecarResp.json();
      // Always boolean
      const result = !!resultBody.result;
      return res.send(sidecarResp.status, { result });
    } catch (err) {
      req.log.error(err);
      return res.send(502, { error: 'Failed to fetch BFF access', message: err.message });
    } finally {
      next();
    }
  }
}


___
// routes/opal/opal-route.js
import { BaseRoute } from '../baseRoute.js';
import jwtDecode from 'jwt-decode';
import config from '../../config/environment/index.js';

export class OpalRoute extends BaseRoute {
  constructor(server, ssoService) {
    super(server);
    this.bindMethods(['postUi', 'postBff']);
    this.createRoutes();
  }

  createRoutes() {
    const base = `${this.baseRoute}/opal`;
    this.server.post(`${base}/ui`,  this.ensureAuthenticated, this.postUi);
    this.server.post(`${base}/bff`, this.ensureAuthenticated, this.postBff);
  }

  // extract roles array from the JWT in req.user.context
  roles(req) {
    try {
      const { custom_group = [] } = jwtDecode(req.user.context);
      return custom_group.filter(r => typeof r === 'string');
    } catch {
      return [];
    }
  }

  // unified side‑car POST helper
  async call(path, payload, req, res) {
    const { host, port } = config.opalClient;
    const url = `http://${host}:${port}${path}`;

    const upstream = await fetch(url, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(payload),
      // reuse your custom agent if you set one in SSOService
      agent:   req._httpMessage.agent,
    });

    let body;
    try { body = await upstream.json(); }
    catch { body = await upstream.text(); }

    return res.send(upstream.status, body);
  }

  // POST /opal/ui  →  { roles }  → side‑car UI endpoint
  postUi(req, res) {
    return this.call(
      config.opalClient.uiPath,
      { roles: this.roles(req) },
      req, res
    );
  }

  // POST /opal/bff  →  { roles, componentType, action }  → side‑car BFF endpoint
  postBff(req, res) {
    const { componentType, action } = req.body;
    if (!componentType || !action) {
      return res.send(400, { error: 'componentType and action are required' });
    }
    return this.call(
      config.opalClient.bffPath,
      { roles: this.roles(req), componentType, action },
      req, res
    );
  }
}
