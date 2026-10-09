/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import http from '../http/index.js';

export function getArticleList(params) {
  return http.get('/cms/articles', { params });
}

export const getPublicArticles = getArticleList;

export function getArticle(id) {
  return http.get(`/cms/articles/${id}`);
}

export const getPublicArticle = getArticle;

export function getArticleComments(id, params) {
  return http.get(`/cms/articles/${id}/comments`, { params });
}

export const getPublicComments = getArticleComments;

export function createArticle(data) {
  return http.post('/cms/articles', data);
}

export const createUserArticle = createArticle;

export function updateArticle(id, data) {
  return http.put(`/cms/articles/${id}`, data);
}

export const updateUserArticle = updateArticle;

export function deleteArticle(id) {
  return http.delete(`/cms/articles/${id}`);
}

export function addComment(id, data) {
  return http.post(`/cms/articles/${id}/comments`, data);
}

export function agreeArticle(id) {
  return http.post(`/cms/articles/${id}/agree`);
}

export function unagreeArticle(id) {
  return http.post(`/cms/articles/${id}/unagree`);
}

export function favoriteArticle(id) {
  return http.post(`/cms/articles/${id}/favorite`);
}

export function unfavoriteArticle(id) {
  return http.post(`/cms/articles/${id}/unfavorite`);
}

export function shareArticle(id) {
  return http.post(`/cms/articles/${id}/share`);
}

export function unshareArticle(id) {
  return http.post(`/cms/articles/${id}/unshare`);
}

export function getMyArticles(params) {
  return http.get('/cms/articles/user/owner', { params });
}

export function getMyFavorites(params) {
  return http.get('/cms/articles/user/favorites', { params });
}

export function getMyAgrees(params) {
  return http.get('/cms/articles/user/agrees', { params });
}

export function getMyComments(params) {
  return http.get('/cms/articles/user/comments', { params });
}

/**
 * Submit a draft for review.
 *
 * No request body: the backend endpoint only takes the article id, so sending a
 * payload here would be silently discarded (D-15).
 */
export function commitArticle(id) {
  return http.post(`/cms/articles/${id}/commit`);
}

/**
 * Resubmit a rejected article.
 *
 * No request body, same reason as {@link commitArticle} (D-15). Review feedback the
 * author needs to act on arrives as a notice, not as a submit-time note.
 */
export function resubmitArticle(id) {
  return http.post(`/cms/articles/${id}/resubmit`);
}

export function reviewArticle(id, { approved, opinion = '' }) {
  return http.post(`/cms/articles/${id}/review`, null, {
    params: { approved, opinion },
  });
}

export function publishArticle(id) {
  return http.post(`/cms/articles/${id}/publish`);
}

export function retractArticle(id) {
  return http.post(`/cms/articles/${id}/retract`);
}

export function listAllArticles(params) {
  return http.get('/cms/articles/listAll', { params });
}

export function listAllComments(params) {
  return http.get('/cms/comments', { params });
}

export function deleteComment(id) {
  return http.delete(`/cms/comments/${id}`);
}

export function averseArticle(id) {
  return http.post(`/cms/articles/${id}/averse`);
}

export function unaverseArticle(id) {
  return http.post(`/cms/articles/${id}/unaverse`);
}

export function getMyAverses(params) {
  return http.get('/cms/articles/user/averses', { params });
}

export const getMyDislikes = getMyAverses;

export function getArticleInteractions(id) {
  return http.get(`/cms/articles/${id}/tally`);
}

export function dislikeArticle(id) {
  return http.post(`/cms/articles/${id}/averse`);
}

export function undislikeArticle(id) {
  return http.post(`/cms/articles/${id}/unaverse`);
}

export const articleApi = {
  getArticleList,
  getPublicArticles,
  getArticle,
  getPublicArticle,
  getArticleComments,
  getPublicComments,
  createArticle,
  createUserArticle,
  updateArticle,
  updateUserArticle,
  deleteArticle,
  addComment,
  agreeArticle,
  unagreeArticle,
  favoriteArticle,
  unfavoriteArticle,
  shareArticle,
  unshareArticle,
  getMyArticles,
  getMyFavorites,
  getMyAgrees,
  getMyComments,
  commitArticle,
  resubmitArticle,
  reviewArticle,
  publishArticle,
  retractArticle,
  listAllArticles,
  listAllComments,
  deleteComment,
  dislikeArticle,
  undislikeArticle,
  averseArticle,
  unaverseArticle,
  getMyAverses,
  getMyDislikes,
  getArticleInteractions,
};
