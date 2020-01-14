import {
  REGISTER_REGION,
  SET_ACTIVE_REGION_ENTITY,
} from '../constants/regions';
import createActionHelper from './createActionHelper';

export const registerRegion = (regionId, initProps) =>
  createActionHelper(REGISTER_REGION)({ regionId, initProps });

export const setActiveEntity = (regionId, activeEntity) =>
  createActionHelper(SET_ACTIVE_REGION_ENTITY)({ regionId, activeEntity });
